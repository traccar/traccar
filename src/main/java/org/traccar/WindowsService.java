/*
 * Copyright 2018 - 2020 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.HandlerEx;
import com.sun.jna.platform.win32.Winsvc.SC_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_DESCRIPTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_MAIN_FUNCTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_TABLE_ENTRY;
import jnr.posix.POSIXFactory;

import java.io.File;
import java.net.URISyntaxException;

public abstract class WindowsService {

    private static final Advapi32 ADVAPI_32 = Advapi32.INSTANCE;

    private final Object waitObject = new Object();

    private final String serviceName;
    private SERVICE_STATUS_HANDLE serviceStatusHandle;

    public WindowsService(String serviceName) {
        this.serviceName = serviceName;
    }

    public void install(
            String displayName, String description, String[] dependencies,
            String account, String password, String config) throws URISyntaxException {

        String javaHome = System.getProperty("java.home");
        String javaBinary = "\"" + javaHome + "\\bin\\java.exe\"";

        File jar = new File(WindowsService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String command = javaBinary
                + " -Duser.dir=\"" + jar.getParentFile().getAbsolutePath() + "\""
                + " -jar \"" + jar.getAbsolutePath() + "\""
                + " --service \"" + config + "\"";

        StringBuilder dep = new StringBuilder();

        if (dependencies != null) {
            for (String s : dependencies) {
                dep.append(s);
                dep.append("\0");
            }
        }
        dep.append("\0");

        SERVICE_DESCRIPTION desc = new SERVICE_DESCRIPTION();
        desc.lpDescription = description;

        SC_HANDLE serviceManager = openServiceControlManager(null, Winsvc.SC_MANAGER_ALL_ACCESS);

        if (serviceManager != null) {
            SC_HANDLE service = ADVAPI_32.CreateService(serviceManager, serviceName, displayName,
                    Winsvc.SERVICE_ALL_ACCESS, WinNT.SERVICE_WIN32_OWN_PROCESS, WinNT.SERVICE_AUTO_START,
                    WinNT.SERVICE_ERROR_NORMAL,
                    command,
                    null, null, dep.toString(), account, password);

            if (service != null) {
                ADVAPI_32.ChangeServiceConfig2(service, Winsvc.SERVICE_CONFIG_DESCRIPTION, desc);
                ADVAPI_32.CloseServiceHandle(service);
            }
            ADVAPI_32.CloseServiceHandle(serviceManager);
        }
    }

    public void uninstall() {
        SC_HANDLE serviceManager = openServiceControlManager(null, Winsvc.SC_MANAGER_ALL_ACCESS);

        if (serviceManager != null) {
            SC_HANDLE service = ADVAPI_32.OpenService(serviceManager, serviceName, Winsvc.SERVICE_ALL_ACCESS);

            if (service != null) {
                ADVAPI_32.DeleteService(service);
                ADVAPI_32.CloseServiceHandle(service);
            }
            ADVAPI_32.CloseServiceHandle(serviceManager);
        }
    }

    public boolean start() {
        boolean success = false;

        SC_HANDLE serviceManager = openServiceControlManager(null, WinNT.GENERIC_EXECUTE);

        if (serviceManager != null) {
            SC_HANDLE service = ADVAPI_32.OpenService(serviceManager, serviceName, WinNT.GENERIC_EXECUTE);

            if (service != null) {
                success = ADVAPI_32.StartService(service, 0, null);
                ADVAPI_32.CloseServiceHandle(service);
            }
            ADVAPI_32.CloseServiceHandle(serviceManager);
        }

        return success;
    }

    public boolean stop() {
        boolean success = false;

        SC_HANDLE serviceManager = openServiceControlManager(null, WinNT.GENERIC_EXECUTE);

        if (serviceManager != null) {
            SC_HANDLE service = Advapi32.INSTANCE.OpenService(serviceManager, serviceName, WinNT.GENERIC_EXECUTE);

            if (service != null) {
                SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
                success = Advapi32.INSTANCE.ControlService(service, Winsvc.SERVICE_CONTROL_STOP, serviceStatus);
                Advapi32.INSTANCE.CloseServiceHandle(service);
            }
            Advapi32.INSTANCE.CloseServiceHandle(serviceManager);
        }

        return success;
    }

    public void init() throws URISyntaxException {
        String path = new File(
                WindowsService.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

        POSIXFactory.getPOSIX().chdir(path);

        ServiceMain serviceMain = new ServiceMain();
        SERVICE_TABLE_ENTRY entry = new SERVICE_TABLE_ENTRY();
        entry.lpServiceName = serviceName;
        entry.lpServiceProc = serviceMain;

        Advapi32.INSTANCE.StartServiceCtrlDispatcher((SERVICE_TABLE_ENTRY[]) entry.toArray(2));
    }

    private SC_HANDLE openServiceControlManager(String machine, int access) {
        return ADVAPI_32.OpenSCManager(machine, null, access);
    }

    private void reportStatus(int status, int win32ExitCode, int waitHint) {
        SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
        serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
        serviceStatus.dwControlsAccepted = Winsvc.SERVICE_ACCEPT_STOP | Winsvc.SERVICE_ACCEPT_SHUTDOWN;
        serviceStatus.dwWin32ExitCode = win32ExitCode;
        serviceStatus.dwWaitHint = waitHint;
        serviceStatus.dwCurrentState = status;

        ADVAPI_32.SetServiceStatus(serviceStatusHandle, serviceStatus);
    }

    public abstract void run();

    private class ServiceMain implements SERVICE_MAIN_FUNCTION {

        public void callback(int dwArgc, Pointer lpszArgv) {
            ServiceControl serviceControl = new ServiceControl();
            serviceStatusHandle = ADVAPI_32.RegisterServiceCtrlHandlerEx(serviceName, serviceControl, null);

            reportStatus(Winsvc.SERVICE_START_PENDING, WinError.NO_ERROR, 3000);
            reportStatus(Winsvc.SERVICE_RUNNING, WinError.NO_ERROR, 0);

            Thread.currentThread().setContextClassLoader(WindowsService.class.getClassLoader());

            run();

            try {
                synchronized (waitObject) {
                    waitObject.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            reportStatus(Winsvc.SERVICE_STOPPED, WinError.NO_ERROR, 0);

            // Avoid returning from ServiceMain, which will cause a crash
            // See http://support.microsoft.com/kb/201349, which recommends
            // having init() wait for this thread.
            // Waiting on this thread in init() won't fix the crash, though.

            System.exit(0);
        }

    }

    private class ServiceControl implements HandlerEx {

        public int callback(int dwControl, int dwEventType, Pointer lpEventData, Pointer lpContext) {
            switch (dwControl) {
                case Winsvc.SERVICE_CONTROL_STOP:
                case Winsvc.SERVICE_CONTROL_SHUTDOWN:
                    reportStatus(Winsvc.SERVICE_STOP_PENDING, WinError.NO_ERROR, 5000);
                    synchronized (waitObject) {
                        waitObject.notifyAll();
                    }
                    break;
                default:
                    break;
            }
            return WinError.NO_ERROR;
        }

    }

}
