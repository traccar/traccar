/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.reports.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.security.PermissionsService;
import org.traccar.mail.MailManager;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import jakarta.activation.DataHandler;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.util.ByteArrayDataSource;

public class ReportMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportMailer.class);

    private final PermissionsService permissionsService;
    private final MailManager mailManager;

    @Inject
    public ReportMailer(PermissionsService permissionsService, MailManager mailManager) {
        this.permissionsService = permissionsService;
        this.mailManager = mailManager;
    }

    private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    public void sendAsync(long userId, ReportExecutor executor) {
        new Thread(() -> {
            try {
                var stream = new ByteArrayOutputStream();
                executor.execute(stream);

                MimeBodyPart attachment = new MimeBodyPart();
                attachment.setFileName("report.xlsx");
                attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        stream.toByteArray(), "application/octet-stream")));

                User user = permissionsService.getUser(userId);
                mailManager.sendMessage(user, false, "Report", "The report is in the attachment.", attachment);
            } catch (StorageException | IOException | MessagingException e) {
                LOGGER.warn("Email report failed", e);
            }
        }).start();
    }

    public void sendAsync(long userId, ReportExecutor executor, String type, Date from, Date to, List<Device> devices,
            List<Group> groups, TimeZone timeZone) {
        new Thread(() -> {
            try {
                formatter.setTimeZone(timeZone);
                var stream = new ByteArrayOutputStream();
                executor.execute(stream);

                MimeBodyPart attachment = new MimeBodyPart();

                attachment.setFileName("report" + "-" + type + ".xlsx");
                attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        stream.toByteArray(), "application/octet-stream")));

                User user = permissionsService.getUser(userId);

                String appendage = "";

                if (type.equals("trips")) {
                    appendage += " Trips ";
                } else if (type.equals("route")) {
                    appendage += " Routes ";
                } else if (type.equals("summary")) {
                    appendage += " Summary ";
                } else if (type.equals("summary-daily")) {
                    appendage += " Summary (Daily) ";
                } else if (type.equals("stops")) {
                    appendage += " Stops ";
                } else if (type.equals("events")) {
                    appendage += " Events ";
                } else
                    appendage += " ";
                appendage += "(" + formatter.format(from) + " to " + formatter.format(from) + ")";

                String bodyString = "Report" + appendage + "<br />";
                if (devices.size() > 0) {
                    bodyString += "<br />Devices:<br />";
                    for (var device : devices) {
                        bodyString += device.getName() + "<br />";
                    }
                }

                // if group size is greater than 0 then add the groups to the bodyString
                if (groups.size() > 0) {
                    bodyString += "<br />Groups:<br />";
                    for (var group : groups) {
                        bodyString += group.getName() + "<br />";
                    }
                }

                bodyString += "\nThe report is in the attachment.";

                mailManager.sendMessage(user,
                        false,
                        // --> Subject of the email:
                        /*
                         * Report - <type> (<from> to <to>)
                         */

                        "Report" + appendage,

                        // --> Body of the email:
                        /*
                         * Report - <type> (<from> to <to>)
                         *
                         * Devices: (if any)
                         * <device1>
                         * <device2>
                         *
                         * Groups: (if any)
                         * <group1>
                         * <group2>
                         *
                         * The report is in the attachment.
                         */
                        bodyString, attachment);
            } catch (StorageException | IOException | MessagingException e) {
                LOGGER.warn("Email report failed", e);
            }
        }).start();
    }

}
