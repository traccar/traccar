[Setup]
AppName=Traccar
AppVersion=3.8
DefaultDirName={pf}\Traccar
AlwaysRestart=yes
OutputBaseFilename=traccar-setup

[Dirs]
Name: "{app}\bat"
Name: "{app}\conf"
Name: "{app}\data"
Name: "{app}\lib"
Name: "{app}\logs"
Name: "{app}\web"
Name: "{app}\schema"
Name: "{app}\templates"

[Files]
Source: "out\*"; DestDir: "{app}"; Flags: recursesubdirs

[Run]
Filename: "{app}\bat\installService.bat"

[UninstallRun]
Filename: "{app}\bat\uninstallService.bat"

[Code]
function GetLocalMachine(): Integer;
begin
  if IsWin64 then
  begin
    Result := HKLM64;
  end
  else
  begin
    Result := HKEY_LOCAL_MACHINE;
  end;
end;

function InitializeSetup(): Boolean;
begin
  if RegKeyExists(GetLocalMachine(), 'SOFTWARE\JavaSoft\Java Runtime Environment') then
  begin
    Result := true;
  end
  else
  begin
    Result := false;
    MsgBox('This application requires Java Runtime Environment version 7 or later. Please download and install the JRE and run this setup again. If you have Java installed and still get this error, you need to re-install it from offline installer (for more info see https://www.traccar.org/windows/).', mbCriticalError, MB_OK);
  end;
end;
