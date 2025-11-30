/*
SQLyog Enterprise - MySQL GUI v8.05 
MySQL - 5.5.5-10.6.22-MariaDB-0ubuntu0.22.04.1 : Database - traccar
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `DATABASECHANGELOG` */

CREATE TABLE `DATABASECHANGELOG` (
  `ID` varchar(255) NOT NULL,
  `AUTHOR` varchar(255) NOT NULL,
  `FILENAME` varchar(255) NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int(11) NOT NULL,
  `EXECTYPE` varchar(10) NOT NULL,
  `MD5SUM` varchar(35) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `COMMENTS` varchar(255) DEFAULT NULL,
  `TAG` varchar(255) DEFAULT NULL,
  `LIQUIBASE` varchar(20) DEFAULT NULL,
  `CONTEXTS` varchar(255) DEFAULT NULL,
  `LABELS` varchar(255) DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `DATABASECHANGELOGLOCK` */

CREATE TABLE `DATABASECHANGELOGLOCK` (
  `ID` int(11) NOT NULL,
  `LOCKED` bit(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_attributes` */

CREATE TABLE `tc_attributes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(4000) NOT NULL,
  `type` varchar(128) NOT NULL,
  `attribute` varchar(128) NOT NULL,
  `expression` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_calendars` */

CREATE TABLE `tc_calendars` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `data` mediumblob NOT NULL,
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_commands` */

CREATE TABLE `tc_commands` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(4000) NOT NULL,
  `type` varchar(128) NOT NULL,
  `textchannel` bit(1) NOT NULL DEFAULT b'0',
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_commands_queue` */

CREATE TABLE `tc_commands_queue` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `deviceid` int(11) NOT NULL,
  `type` varchar(128) NOT NULL,
  `textchannel` bit(1) NOT NULL DEFAULT b'0',
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_commands_queue_deviceid` (`deviceid`),
  CONSTRAINT `fk_commands_queue_deviceid` FOREIGN KEY (`deviceid`) REFERENCES `tc_devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_attribute` */

CREATE TABLE `tc_device_attribute` (
  `deviceid` int(11) NOT NULL,
  `attributeid` int(11) NOT NULL,
  KEY `fk_user_device_attribute_attributeid` (`attributeid`),
  KEY `fk_user_device_attribute_deviceid` (`deviceid`),
  CONSTRAINT `fk_user_device_attribute_attributeid` FOREIGN KEY (`attributeid`) REFERENCES `tc_attributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_command` */

CREATE TABLE `tc_device_command` (
  `deviceid` int(11) NOT NULL,
  `commandid` int(11) NOT NULL,
  KEY `fk_device_command_commandid` (`commandid`),
  KEY `fk_device_command_deviceid` (`deviceid`),
  CONSTRAINT `fk_device_command_commandid` FOREIGN KEY (`commandid`) REFERENCES `tc_commands` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_driver` */

CREATE TABLE `tc_device_driver` (
  `deviceid` int(11) NOT NULL,
  `driverid` int(11) NOT NULL,
  KEY `fk_device_driver_deviceid` (`deviceid`),
  KEY `fk_device_driver_driverid` (`driverid`),
  CONSTRAINT `fk_device_driver_driverid` FOREIGN KEY (`driverid`) REFERENCES `tc_drivers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_geofence` */

CREATE TABLE `tc_device_geofence` (
  `deviceid` int(11) NOT NULL,
  `geofenceid` int(11) NOT NULL,
  KEY `fk_device_geofence_deviceid` (`deviceid`),
  KEY `fk_device_geofence_geofenceid` (`geofenceid`),
  CONSTRAINT `fk_device_geofence_geofenceid` FOREIGN KEY (`geofenceid`) REFERENCES `tc_geofences` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_maintenance` */

CREATE TABLE `tc_device_maintenance` (
  `deviceid` int(11) NOT NULL,
  `maintenanceid` int(11) NOT NULL,
  KEY `fk_device_maintenance_deviceid` (`deviceid`),
  KEY `fk_device_maintenance_maintenanceid` (`maintenanceid`),
  CONSTRAINT `fk_device_maintenance_maintenanceid` FOREIGN KEY (`maintenanceid`) REFERENCES `tc_maintenances` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_notification` */

CREATE TABLE `tc_device_notification` (
  `deviceid` int(11) NOT NULL,
  `notificationid` int(11) NOT NULL,
  KEY `fk_device_notification_deviceid` (`deviceid`),
  KEY `fk_device_notification_notificationid` (`notificationid`),
  CONSTRAINT `fk_device_notification_notificationid` FOREIGN KEY (`notificationid`) REFERENCES `tc_notifications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_order` */

CREATE TABLE `tc_device_order` (
  `deviceid` int(11) NOT NULL,
  `orderid` int(11) NOT NULL,
  KEY `fk_device_order_deviceid` (`deviceid`),
  KEY `fk_device_order_orderid` (`orderid`),
  CONSTRAINT `fk_device_order_deviceid` FOREIGN KEY (`deviceid`) REFERENCES `tc_devices` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_device_order_orderid` FOREIGN KEY (`orderid`) REFERENCES `tc_orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_device_report` */

CREATE TABLE `tc_device_report` (
  `deviceid` int(11) NOT NULL,
  `reportid` int(11) NOT NULL,
  KEY `fk_device_report_deviceid` (`deviceid`),
  KEY `fk_device_report_reportid` (`reportid`),
  CONSTRAINT `fk_device_report_deviceid` FOREIGN KEY (`deviceid`) REFERENCES `tc_devices` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_device_report_reportid` FOREIGN KEY (`reportid`) REFERENCES `tc_reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_devices` */

CREATE TABLE `tc_devices` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `uniqueid` varchar(128) NOT NULL,
  `lastupdate` timestamp NULL DEFAULT NULL,
  `positionid` int(11) DEFAULT NULL,
  `groupid` int(11) DEFAULT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `phone` varchar(128) DEFAULT NULL,
  `model` varchar(128) DEFAULT NULL,
  `contact` varchar(512) DEFAULT NULL,
  `category` varchar(128) DEFAULT NULL,
  `disabled` bit(1) DEFAULT b'0',
  `status` char(8) DEFAULT NULL,
  `expirationtime` timestamp NULL DEFAULT NULL,
  `motionstate` bit(1) DEFAULT b'0',
  `motiontime` timestamp NULL DEFAULT NULL,
  `motiondistance` double DEFAULT 0,
  `overspeedstate` bit(1) DEFAULT b'0',
  `overspeedtime` timestamp NULL DEFAULT NULL,
  `overspeedgeofenceid` int(11) DEFAULT 0,
  `motionstreak` bit(1) DEFAULT b'0',
  `calendarid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_devices_groupid` (`groupid`),
  KEY `idx_devices_uniqueid` (`uniqueid`),
  KEY `fk_devices_calendarid` (`calendarid`),
  CONSTRAINT `fk_devices_calendarid` FOREIGN KEY (`calendarid`) REFERENCES `tc_calendars` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_devices_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_drivers` */

CREATE TABLE `tc_drivers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `uniqueid` varchar(128) NOT NULL,
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqueid` (`uniqueid`),
  KEY `idx_drivers_uniqueid` (`uniqueid`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_events` */

CREATE TABLE `tc_events` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(128) NOT NULL,
  `eventtime` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deviceid` int(11) DEFAULT NULL,
  `positionid` int(11) DEFAULT NULL,
  `geofenceid` int(11) DEFAULT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `maintenanceid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_events_deviceid` (`deviceid`),
  KEY `event_deviceid_servertime` (`deviceid`,`eventtime`),
  CONSTRAINT `fk_events_deviceid` FOREIGN KEY (`deviceid`) REFERENCES `tc_devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=357561 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_geofences` */

CREATE TABLE `tc_geofences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `description` varchar(128) DEFAULT NULL,
  `area` varchar(4096) NOT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `calendarid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_geofence_calendar_calendarid` (`calendarid`),
  CONSTRAINT `fk_geofence_calendar_calendarid` FOREIGN KEY (`calendarid`) REFERENCES `tc_calendars` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_attribute` */

CREATE TABLE `tc_group_attribute` (
  `groupid` int(11) NOT NULL,
  `attributeid` int(11) NOT NULL,
  KEY `fk_group_attribute_attributeid` (`attributeid`),
  KEY `fk_group_attribute_groupid` (`groupid`),
  CONSTRAINT `fk_group_attribute_attributeid` FOREIGN KEY (`attributeid`) REFERENCES `tc_attributes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_attribute_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_command` */

CREATE TABLE `tc_group_command` (
  `groupid` int(11) NOT NULL,
  `commandid` int(11) NOT NULL,
  KEY `fk_group_command_commandid` (`commandid`),
  KEY `fk_group_command_groupid` (`groupid`),
  CONSTRAINT `fk_group_command_commandid` FOREIGN KEY (`commandid`) REFERENCES `tc_commands` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_command_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_driver` */

CREATE TABLE `tc_group_driver` (
  `groupid` int(11) NOT NULL,
  `driverid` int(11) NOT NULL,
  KEY `fk_group_driver_driverid` (`driverid`),
  KEY `fk_group_driver_groupid` (`groupid`),
  CONSTRAINT `fk_group_driver_driverid` FOREIGN KEY (`driverid`) REFERENCES `tc_drivers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_driver_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_geofence` */

CREATE TABLE `tc_group_geofence` (
  `groupid` int(11) NOT NULL,
  `geofenceid` int(11) NOT NULL,
  KEY `fk_group_geofence_geofenceid` (`geofenceid`),
  KEY `fk_group_geofence_groupid` (`groupid`),
  CONSTRAINT `fk_group_geofence_geofenceid` FOREIGN KEY (`geofenceid`) REFERENCES `tc_geofences` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_geofence_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_maintenance` */

CREATE TABLE `tc_group_maintenance` (
  `groupid` int(11) NOT NULL,
  `maintenanceid` int(11) NOT NULL,
  KEY `fk_group_maintenance_groupid` (`groupid`),
  KEY `fk_group_maintenance_maintenanceid` (`maintenanceid`),
  CONSTRAINT `fk_group_maintenance_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_maintenance_maintenanceid` FOREIGN KEY (`maintenanceid`) REFERENCES `tc_maintenances` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_notification` */

CREATE TABLE `tc_group_notification` (
  `groupid` int(11) NOT NULL,
  `notificationid` int(11) NOT NULL,
  KEY `fk_group_notification_groupid` (`groupid`),
  KEY `fk_group_notification_notificationid` (`notificationid`),
  CONSTRAINT `fk_group_notification_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_notification_notificationid` FOREIGN KEY (`notificationid`) REFERENCES `tc_notifications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_order` */

CREATE TABLE `tc_group_order` (
  `groupid` int(11) NOT NULL,
  `orderid` int(11) NOT NULL,
  KEY `fk_group_order_groupid` (`groupid`),
  KEY `fk_group_order_orderid` (`orderid`),
  CONSTRAINT `fk_group_order_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_order_orderid` FOREIGN KEY (`orderid`) REFERENCES `tc_orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_group_report` */

CREATE TABLE `tc_group_report` (
  `groupid` int(11) NOT NULL,
  `reportid` int(11) NOT NULL,
  KEY `fk_group_report_groupid` (`groupid`),
  KEY `fk_group_report_reportid` (`reportid`),
  CONSTRAINT `fk_group_report_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_report_reportid` FOREIGN KEY (`reportid`) REFERENCES `tc_reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_groups` */

CREATE TABLE `tc_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `groupid` int(11) DEFAULT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_groups_groupid` (`groupid`),
  CONSTRAINT `fk_groups_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_keystore` */

CREATE TABLE `tc_keystore` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `publickey` mediumblob NOT NULL,
  `privatekey` mediumblob NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_maintenances` */

CREATE TABLE `tc_maintenances` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(4000) NOT NULL,
  `type` varchar(128) NOT NULL,
  `start` double NOT NULL DEFAULT 0,
  `period` double NOT NULL DEFAULT 0,
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_notifications` */

CREATE TABLE `tc_notifications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(128) NOT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `always` bit(1) NOT NULL DEFAULT b'0',
  `calendarid` int(11) DEFAULT NULL,
  `notificators` varchar(128) DEFAULT NULL,
  `commandid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notification_calendar_calendarid` (`calendarid`),
  KEY `fk_notifications_commandid` (`commandid`),
  CONSTRAINT `fk_notification_calendar_calendarid` FOREIGN KEY (`calendarid`) REFERENCES `tc_calendars` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_notifications_commandid` FOREIGN KEY (`commandid`) REFERENCES `tc_commands` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_orders` */

CREATE TABLE `tc_orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uniqueid` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `fromaddress` varchar(512) DEFAULT NULL,
  `toaddress` varchar(512) DEFAULT NULL,
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_positions` */

CREATE TABLE `tc_positions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `protocol` varchar(128) DEFAULT NULL,
  `deviceid` int(11) NOT NULL,
  `servertime` timestamp NOT NULL DEFAULT current_timestamp(),
  `devicetime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `fixtime` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `valid` bit(1) NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `altitude` float NOT NULL,
  `speed` float NOT NULL,
  `course` float NOT NULL,
  `address` varchar(512) DEFAULT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `accuracy` double NOT NULL DEFAULT 0,
  `network` varchar(4000) DEFAULT NULL,
  `geofenceids` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_positions_deviceid` (`deviceid`),
  KEY `position_deviceid_fixtime` (`deviceid`,`fixtime`)
) ENGINE=InnoDB AUTO_INCREMENT=312899 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_reports` */

CREATE TABLE `tc_reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(32) NOT NULL,
  `description` varchar(128) NOT NULL,
  `calendarid` int(11) NOT NULL,
  `attributes` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_reports_calendarid` (`calendarid`),
  CONSTRAINT `fk_reports_calendarid` FOREIGN KEY (`calendarid`) REFERENCES `tc_calendars` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_servers` */

CREATE TABLE `tc_servers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `registration` bit(1) NOT NULL DEFAULT b'1',
  `latitude` double NOT NULL DEFAULT 0,
  `longitude` double NOT NULL DEFAULT 0,
  `zoom` int(11) NOT NULL DEFAULT 0,
  `map` varchar(128) DEFAULT NULL,
  `bingkey` varchar(128) DEFAULT NULL,
  `mapurl` varchar(512) DEFAULT NULL,
  `readonly` bit(1) NOT NULL DEFAULT b'0',
  `twelvehourformat` bit(1) NOT NULL DEFAULT b'0',
  `attributes` varchar(4000) DEFAULT NULL,
  `forcesettings` bit(1) NOT NULL DEFAULT b'0',
  `coordinateformat` varchar(128) DEFAULT NULL,
  `devicereadonly` bit(1) DEFAULT b'0',
  `limitcommands` bit(1) DEFAULT b'0',
  `poilayer` varchar(512) DEFAULT NULL,
  `announcement` varchar(4000) DEFAULT NULL,
  `disablereports` bit(1) DEFAULT b'0',
  `overlayurl` varchar(512) DEFAULT NULL,
  `fixedemail` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_statistics` */

CREATE TABLE `tc_statistics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `capturetime` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `activeusers` int(11) NOT NULL DEFAULT 0,
  `activedevices` int(11) NOT NULL DEFAULT 0,
  `requests` int(11) NOT NULL DEFAULT 0,
  `messagesreceived` int(11) NOT NULL DEFAULT 0,
  `messagesstored` int(11) NOT NULL DEFAULT 0,
  `attributes` varchar(4096) NOT NULL,
  `mailsent` int(11) NOT NULL DEFAULT 0,
  `smssent` int(11) NOT NULL DEFAULT 0,
  `geocoderrequests` int(11) NOT NULL DEFAULT 0,
  `geolocationrequests` int(11) NOT NULL DEFAULT 0,
  `protocols` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2024 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_attribute` */

CREATE TABLE `tc_user_attribute` (
  `userid` int(11) NOT NULL,
  `attributeid` int(11) NOT NULL,
  KEY `fk_user_attribute_attributeid` (`attributeid`),
  KEY `fk_user_attribute_userid` (`userid`),
  CONSTRAINT `fk_user_attribute_attributeid` FOREIGN KEY (`attributeid`) REFERENCES `tc_attributes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_attribute_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_calendar` */

CREATE TABLE `tc_user_calendar` (
  `userid` int(11) NOT NULL,
  `calendarid` int(11) NOT NULL,
  KEY `fk_user_calendar_calendarid` (`calendarid`),
  KEY `fk_user_calendar_userid` (`userid`),
  CONSTRAINT `fk_user_calendar_calendarid` FOREIGN KEY (`calendarid`) REFERENCES `tc_calendars` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_calendar_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_command` */

CREATE TABLE `tc_user_command` (
  `userid` int(11) NOT NULL,
  `commandid` int(11) NOT NULL,
  KEY `fk_user_command_commandid` (`commandid`),
  KEY `fk_user_command_userid` (`userid`),
  CONSTRAINT `fk_user_command_commandid` FOREIGN KEY (`commandid`) REFERENCES `tc_commands` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_command_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_device` */

CREATE TABLE `tc_user_device` (
  `userid` int(11) NOT NULL,
  `deviceid` int(11) NOT NULL,
  KEY `fk_user_device_deviceid` (`deviceid`),
  KEY `fk_user_device_userid` (`userid`),
  KEY `user_device_user_id` (`userid`),
  CONSTRAINT `fk_user_device_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_driver` */

CREATE TABLE `tc_user_driver` (
  `userid` int(11) NOT NULL,
  `driverid` int(11) NOT NULL,
  KEY `fk_user_driver_driverid` (`driverid`),
  KEY `fk_user_driver_userid` (`userid`),
  CONSTRAINT `fk_user_driver_driverid` FOREIGN KEY (`driverid`) REFERENCES `tc_drivers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_driver_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_geofence` */

CREATE TABLE `tc_user_geofence` (
  `userid` int(11) NOT NULL,
  `geofenceid` int(11) NOT NULL,
  KEY `fk_user_geofence_geofenceid` (`geofenceid`),
  KEY `fk_user_geofence_userid` (`userid`),
  CONSTRAINT `fk_user_geofence_geofenceid` FOREIGN KEY (`geofenceid`) REFERENCES `tc_geofences` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_geofence_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_group` */

CREATE TABLE `tc_user_group` (
  `userid` int(11) NOT NULL,
  `groupid` int(11) NOT NULL,
  KEY `fk_user_group_groupid` (`groupid`),
  KEY `fk_user_group_userid` (`userid`),
  CONSTRAINT `fk_user_group_groupid` FOREIGN KEY (`groupid`) REFERENCES `tc_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_group_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_maintenance` */

CREATE TABLE `tc_user_maintenance` (
  `userid` int(11) NOT NULL,
  `maintenanceid` int(11) NOT NULL,
  KEY `fk_user_maintenance_maintenanceid` (`maintenanceid`),
  KEY `fk_user_maintenance_userid` (`userid`),
  CONSTRAINT `fk_user_maintenance_maintenanceid` FOREIGN KEY (`maintenanceid`) REFERENCES `tc_maintenances` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_maintenance_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_notification` */

CREATE TABLE `tc_user_notification` (
  `userid` int(11) NOT NULL,
  `notificationid` int(11) NOT NULL,
  KEY `fk_user_notification_notificationid` (`notificationid`),
  KEY `fk_user_notification_userid` (`userid`),
  CONSTRAINT `fk_user_notification_notificationid` FOREIGN KEY (`notificationid`) REFERENCES `tc_notifications` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_notification_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_order` */

CREATE TABLE `tc_user_order` (
  `userid` int(11) NOT NULL,
  `orderid` int(11) NOT NULL,
  KEY `fk_user_order_userid` (`userid`),
  KEY `fk_user_order_orderid` (`orderid`),
  CONSTRAINT `fk_user_order_orderid` FOREIGN KEY (`orderid`) REFERENCES `tc_orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_order_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_report` */

CREATE TABLE `tc_user_report` (
  `userid` int(11) NOT NULL,
  `reportid` int(11) NOT NULL,
  KEY `fk_user_report_userid` (`userid`),
  KEY `fk_user_report_reportid` (`reportid`),
  CONSTRAINT `fk_user_report_reportid` FOREIGN KEY (`reportid`) REFERENCES `tc_reports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_report_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_user_user` */

CREATE TABLE `tc_user_user` (
  `userid` int(11) NOT NULL,
  `manageduserid` int(11) NOT NULL,
  KEY `fk_user_user_userid` (`userid`),
  KEY `fk_user_user_manageduserid` (`manageduserid`),
  CONSTRAINT `fk_user_user_manageduserid` FOREIGN KEY (`manageduserid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_user_userid` FOREIGN KEY (`userid`) REFERENCES `tc_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_users` */

CREATE TABLE `tc_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `email` varchar(128) NOT NULL,
  `hashedpassword` varchar(128) DEFAULT NULL,
  `salt` varchar(128) DEFAULT NULL,
  `readonly` bit(1) NOT NULL DEFAULT b'0',
  `administrator` bit(1) DEFAULT NULL,
  `map` varchar(128) DEFAULT NULL,
  `latitude` double NOT NULL DEFAULT 0,
  `longitude` double NOT NULL DEFAULT 0,
  `zoom` int(11) NOT NULL DEFAULT 0,
  `twelvehourformat` bit(1) NOT NULL DEFAULT b'0',
  `attributes` varchar(4000) DEFAULT NULL,
  `coordinateformat` varchar(128) DEFAULT NULL,
  `disabled` bit(1) DEFAULT b'0',
  `expirationtime` timestamp NULL DEFAULT NULL,
  `devicelimit` int(11) DEFAULT -1,
  `userlimit` int(11) DEFAULT 0,
  `devicereadonly` bit(1) DEFAULT b'0',
  `phone` varchar(128) DEFAULT NULL,
  `limitcommands` bit(1) DEFAULT b'0',
  `login` varchar(128) DEFAULT NULL,
  `poilayer` varchar(512) DEFAULT NULL,
  `disablereports` bit(1) DEFAULT b'0',
  `fixedemail` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_login` (`login`)
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/* Function  structure for function  `buscar_mes` */

DELIMITER $$


/*Table structure for table `tc_children_profile` */

CREATE TABLE `tc_children_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `deviceid` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `lastname` varchar(128) DEFAULT NULL,
  `birthdate` date DEFAULT NULL,
  `weight` double DEFAULT NULL,
  `height` double DEFAULT NULL,
  `medicalconditions` text DEFAULT NULL,
  `createdat` timestamp NULL DEFAULT current_timestamp(),
  `updatedat` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_children_profile_deviceid` (`deviceid`),
  CONSTRAINT `fk_children_profile_deviceid` FOREIGN KEY (`deviceid`) REFERENCES `tc_devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

/*Table structure for table `tc_children_health` */

CREATE TABLE `tc_children_health` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `childid` bigint NOT NULL,
  `deviceid` bigint NOT NULL,
  `servertime` timestamp NOT NULL,
  `heartrate` int DEFAULT NULL,
  `bodytemp` double DEFAULT NULL,
  `steps` int DEFAULT NULL,
  `sleepstatus` varchar(64) DEFAULT NULL,
  `rawpositionid` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_children_health_childid` (`childid`),
  KEY `idx_children_health_deviceid_servertime` (`deviceid`,`servertime`),
  CONSTRAINT `fk_children_health_childid` FOREIGN KEY (`childid`) REFERENCES `tc_children_profile` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_children_health_deviceid` FOREIGN KEY (`deviceid`) REFERENCES `tc_devices` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_children_health_positionid` FOREIGN KEY (`rawpositionid`) REFERENCES `tc_positions` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!50003 CREATE DEFINER=`ardangps`@`%` FUNCTION `buscar_mes`(`entra_mes` INT) RETURNS varchar(30) CHARSET latin1 COLLATE latin1_swedish_ci
    NO SQL
BEGIN
DECLARE mes_salida VARCHAR(30);
CASE
WHEN entra_mes = 1 THEN SET mes_salida = "Enero";
WHEN entra_mes = 2 THEN SET mes_salida = "Febrero";
WHEN entra_mes = 3 THEN SET mes_salida = "Marzo";
WHEN entra_mes = 4 THEN SET mes_salida = "Abril";
WHEN entra_mes = 5 THEN SET mes_salida = "Mayo";
WHEN entra_mes = 6 THEN SET mes_salida = "Junio";
WHEN entra_mes = 7 THEN SET mes_salida = "Julio";
WHEN entra_mes = 8 THEN SET mes_salida = "Agosto";
WHEN entra_mes = 9 THEN SET mes_salida = "Septiembre";
WHEN entra_mes = 10 THEN SET mes_salida = "Octubre";
WHEN entra_mes = 11 THEN SET mes_salida = "Noviembre";
WHEN entra_mes = 12 THEN SET mes_salida = "Diciembre";
else SET mes_salida = "";
end CASE;
RETURN mes_salida;
END */$$
DELIMITER ;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
