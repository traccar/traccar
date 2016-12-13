# traccar  
__Version__: 3.9  
__Base URL__: http://traccar.org/api  

### Contents  
[Paths](#paths)  
- [attributes](#paths_attributes)  
  - [PUT /attributes/aliases/{id}](#paths_attributesaliasesid_PUT)  
  - [DELETE /attributes/aliases/{id}](#paths_attributesaliasesid_DELETE)  
  - [POST /attributes/aliases](#paths_attributesaliases_POST)  
  - [GET /attributes/aliases](#paths_attributesaliases_GET)  
- [commands](#paths_commands)  
  - [POST /commands](#paths_commands_POST)  
- [commandtypes](#paths_commandtypes)  
  - [GET /commandtypes](#paths_commandtypes_GET)  
- [devices](#paths_devices)  
  - [POST /devices](#paths_devices_POST)  
  - [GET /devices](#paths_devices_GET)  
  - [POST /devices/geofences](#paths_devicesgeofences_POST)  
  - [DELETE /devices/geofences](#paths_devicesgeofences_DELETE)  
  - [PUT /devices/{id}](#paths_devicesid_PUT)  
  - [DELETE /devices/{id}](#paths_devicesid_DELETE)  
  - [PUT /devices/{id}/distance](#paths_devicesiddistance_PUT)  
- [events](#paths_events)  
  - [GET /events/{id}](#paths_eventsid_GET)  
- [geofences](#paths_geofences)  
  - [PUT /geofences/{id}](#paths_geofencesid_PUT)  
  - [DELETE /geofences/{id}](#paths_geofencesid_DELETE)  
  - [POST /geofences](#paths_geofences_POST)  
  - [GET /geofences](#paths_geofences_GET)  
- [groups](#paths_groups)  
  - [POST /groups/geofences](#paths_groupsgeofences_POST)  
  - [DELETE /groups/geofences](#paths_groupsgeofences_DELETE)  
  - [POST /groups](#paths_groups_POST)  
  - [GET /groups](#paths_groups_GET)  
  - [PUT /groups/{id}](#paths_groupsid_PUT)  
  - [DELETE /groups/{id}](#paths_groupsid_DELETE)  
- [permissions](#paths_permissions)  
  - [POST /permissions/devices](#paths_permissionsdevices_POST)  
  - [DELETE /permissions/devices](#paths_permissionsdevices_DELETE)  
  - [POST /permissions/geofences](#paths_permissionsgeofences_POST)  
  - [DELETE /permissions/geofences](#paths_permissionsgeofences_DELETE)  
  - [POST /permissions/groups](#paths_permissionsgroups_POST)  
  - [DELETE /permissions/groups](#paths_permissionsgroups_DELETE)  
- [positions](#paths_positions)  
  - [GET /positions](#paths_positions_GET)  
- [reports](#paths_reports)  
  - [GET /reports/summary](#paths_reportssummary_GET)  
  - [GET /reports/events](#paths_reportsevents_GET)  
  - [GET /reports/route](#paths_reportsroute_GET)  
  - [GET /reports/trips](#paths_reportstrips_GET)  
- [server](#paths_server)  
  - [PUT /server](#paths_server_PUT)  
  - [GET /server](#paths_server_GET)  
- [session](#paths_session)  
  - [POST /session](#paths_session_POST)  
  - [DELETE /session](#paths_session_DELETE)  
  - [GET /session](#paths_session_GET)  
- [statistics](#paths_statistics)  
  - [GET /statistics](#paths_statistics_GET)  
- [users](#paths_users)  
  - [POST /users/notifications](#paths_usersnotifications_POST)  
  - [GET /users/notifications](#paths_usersnotifications_GET)  
  - [PUT /users/{id}](#paths_usersid_PUT)  
  - [DELETE /users/{id}](#paths_usersid_DELETE)  
  - [POST /users](#paths_users_POST)  
  - [GET /users](#paths_users_GET)  

[Models](#definitions)  
- [AttributeAlias](#definitions_AttributeAlias)  
- [Command](#definitions_Command)  
- [CommandType](#definitions_CommandType)  
- [Device](#definitions_Device)  
- [DeviceGeofence](#definitions_DeviceGeofence)  
- [DevicePermission](#definitions_DevicePermission)  
- [DeviceTotalDistance](#definitions_DeviceTotalDistance)  
- [Event](#definitions_Event)  
- [Geofence](#definitions_Geofence)  
- [GeofencePermission](#definitions_GeofencePermission)  
- [Group](#definitions_Group)  
- [GroupGeofence](#definitions_GroupGeofence)  
- [GroupPermission](#definitions_GroupPermission)  
- [Notification](#definitions_Notification)  
- [Position](#definitions_Position)  
- [ReportSummary](#definitions_ReportSummary)  
- [ReportTrips](#definitions_ReportTrips)  
- [Server](#definitions_Server)  
- [Statistics](#definitions_Statistics)  
- [User](#definitions_User)  

## <a name="paths"></a> Paths  
### <a name="paths_attributes"></a> attributes  
#### <a name="paths_attributesaliasesid_PUT"></a> `PUT /attributes/aliases/{id}`  
__Summary__: Update an AttributeAlias  
##### Parameters  
Name | ParamType | Required | DataType | Schema
--- | --- | --- | --- | ---
_id_ | path | __True__ | _integer_ | 
_body_ | body | __True__ |  | [AttributeAlias](#definitions_AttributeAlias)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [AttributeAlias](#definitions_AttributeAlias)
#### <a name="paths_attributesaliasesid_DELETE"></a> `DELETE /attributes/aliases/{id}`  
__Summary__: Delete an AttributeAlias  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_id_ | path | __True__ | _integer_
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_attributesaliases_POST"></a> `POST /attributes/aliases`  
__Summary__: Set an AttributeAlias  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [AttributeAlias](#definitions_AttributeAlias)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [AttributeAlias](#definitions_AttributeAlias)
#### <a name="paths_attributesaliases_GET"></a> `GET /attributes/aliases`  
__Summary__: Fetch a list of AttributeAlias  
__Description__: Without params, it returns a list of AttributeAlias from all the user's Devices  
##### Parameters  
Name | ParamType | Description | DataType
--- | --- | --- | ---
_deviceId_ | query | Standard users can use this only with _userId_s, they have access to | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [AttributeAlias](#definitions_AttributeAlias) ]

### <a name="paths_commands"></a> commands  
#### <a name="paths_commands_POST"></a> `POST /commands`  
__Summary__: Dispatch commands to device  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [Command](#definitions_Command)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Command](#definitions_Command)
_400_ | Could happen when dispatching to a device that is offline, the user doesn't have permission or an incorrect command _type_ for the device | 

### <a name="paths_commandtypes"></a> commandtypes  
#### <a name="paths_commandtypes_GET"></a> `GET /commandtypes`  
__Summary__: Fetch a list of available Commands for the Device  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_deviceId_ | query | __True__ | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [CommandType](#definitions_CommandType) ]
_400_ | Could happen when trying to fetch from an pffline device or the user does not have permission | 

### <a name="paths_devices"></a> devices  
#### <a name="paths_devices_POST"></a> `POST /devices`  
__Summary__: Create a Device  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [Device](#definitions_Device)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Device](#definitions_Device)
#### <a name="paths_devices_GET"></a> `GET /devices`  
__Summary__: Fetch a list of Devices  
__Description__: Without any params, returns a list of the user's devices  
##### Parameters  
Name | ParamType | Description | DataType
--- | --- | --- | ---
_all_ | query | Can only be used by admin users to fetch all entities | _boolean_
_userId_ | query | Standard users can use this only with their own _userId_ | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Device](#definitions_Device) ]
_400_ | No permission | 
#### <a name="paths_devicesgeofences_POST"></a> `POST /devices/geofences`  
__Summary__: Link a Geofence to a Device  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [DeviceGeofence](#definitions_DeviceGeofence)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [DeviceGeofence](#definitions_DeviceGeofence)
#### <a name="paths_devicesgeofences_DELETE"></a> `DELETE /devices/geofences`  
__Summary__: Remove a Geofence from a Device  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [DeviceGeofence](#definitions_DeviceGeofence)
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_devicesid_PUT"></a> `PUT /devices/{id}`  
__Summary__: Update a Device  
##### Parameters  
Name | ParamType | Required | DataType | Schema
--- | --- | --- | --- | ---
_id_ | path | __True__ | _integer_ | 
_body_ | body | __True__ |  | [Device](#definitions_Device)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Device](#definitions_Device)
#### <a name="paths_devicesid_DELETE"></a> `DELETE /devices/{id}`  
__Summary__: Update a Device  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_id_ | path | __True__ | _integer_
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_devicesiddistance_PUT"></a> `PUT /devices/{id}/distance`  
__Summary__: Update the distance counter of the Device  
##### Parameters  
Name | ParamType | Required | DataType | Schema
--- | --- | --- | --- | ---
_id_ | path | __True__ | _integer_ | 
_body_ | body | __True__ |  | [DeviceTotalDistance](#definitions_DeviceTotalDistance)
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content

### <a name="paths_events"></a> events  
#### <a name="paths_eventsid_GET"></a> `GET /events/{id}`  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_id_ | path | __True__ | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Event](#definitions_Event)

### <a name="paths_geofences"></a> geofences  
#### <a name="paths_geofencesid_PUT"></a> `PUT /geofences/{id}`  
__Summary__: Update a Geofence  
##### Parameters  
Name | ParamType | Required | DataType | Schema
--- | --- | --- | --- | ---
_id_ | path | __True__ | _integer_ | 
_body_ | body | __True__ |  | [Geofence](#definitions_Geofence)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Geofence](#definitions_Geofence)
#### <a name="paths_geofencesid_DELETE"></a> `DELETE /geofences/{id}`  
__Summary__: Delete a Geofence  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_id_ | path | __True__ | _integer_
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_geofences_POST"></a> `POST /geofences`  
__Summary__: Create a Geofence  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [Geofence](#definitions_Geofence)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Geofence](#definitions_Geofence)
#### <a name="paths_geofences_GET"></a> `GET /geofences`  
__Summary__: Fetch a list of Geofences  
__Description__: Without params, it returns a list of Geofences the user has access to  
##### Parameters  
Name | ParamType | Description | Required | DataType
--- | --- | --- | --- | ---
_all_ | query | Can only be used by admin users to fetch all entities |  | _boolean_
_userId_ | query | Standard users can use this only with their own _userId_ |  | _integer_
_groupId_ | query |  |  | _integer_
_deviceId_ | query | Standard users can use this only with _userId_s, they have access to |  | _integer_
_refresh_ | query |  | False | _boolean_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Geofence](#definitions_Geofence) ]

### <a name="paths_groups"></a> groups  
#### <a name="paths_groupsgeofences_POST"></a> `POST /groups/geofences`  
__Summary__: Link a Geofence to a Group  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [GroupGeofence](#definitions_GroupGeofence)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [GroupGeofence](#definitions_GroupGeofence)
#### <a name="paths_groupsgeofences_DELETE"></a> `DELETE /groups/geofences`  
__Summary__: Remove a Geofence from a Group  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [GroupGeofence](#definitions_GroupGeofence)
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_groups_POST"></a> `POST /groups`  
__Summary__: Create a Group  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [Group](#definitions_Group)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Group](#definitions_Group)
_400_ | No permission | 
#### <a name="paths_groups_GET"></a> `GET /groups`  
__Summary__: Fetch a list of Groups  
__Description__: Without any params, returns a list of the Groups the user belongs to  
##### Parameters  
Name | ParamType | Description | DataType
--- | --- | --- | ---
_all_ | query | Can only be used by admin users to fetch all entities | _boolean_
_userId_ | query | Standard users can use this only with their own _userId_ | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Group](#definitions_Group) ]
#### <a name="paths_groupsid_PUT"></a> `PUT /groups/{id}`  
__Summary__: Update a Group  
##### Parameters  
Name | ParamType | Required | DataType | Schema
--- | --- | --- | --- | ---
_id_ | path | __True__ | _integer_ | 
_body_ | body | __True__ |  | [Group](#definitions_Group)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Group](#definitions_Group)
#### <a name="paths_groupsid_DELETE"></a> `DELETE /groups/{id}`  
__Summary__: Delete a Group  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_id_ | path | __True__ | _integer_
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content

### <a name="paths_permissions"></a> permissions  
#### <a name="paths_permissionsdevices_POST"></a> `POST /permissions/devices`  
__Summary__: Link a Device to a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [DevicePermission](#definitions_DevicePermission)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [DevicePermission](#definitions_DevicePermission)
_400_ | No permission | 
#### <a name="paths_permissionsdevices_DELETE"></a> `DELETE /permissions/devices`  
__Summary__: Remove a Device from a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [DevicePermission](#definitions_DevicePermission)
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_permissionsgeofences_POST"></a> `POST /permissions/geofences`  
__Summary__: Link a Geofence to a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [GeofencePermission](#definitions_GeofencePermission)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [GeofencePermission](#definitions_GeofencePermission)
#### <a name="paths_permissionsgeofences_DELETE"></a> `DELETE /permissions/geofences`  
__Summary__: Remove a Geofence from a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [GeofencePermission](#definitions_GeofencePermission)
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_permissionsgroups_POST"></a> `POST /permissions/groups`  
__Summary__: Link a Group to a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [GroupPermission](#definitions_GroupPermission)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [GroupPermission](#definitions_GroupPermission)
#### <a name="paths_permissionsgroups_DELETE"></a> `DELETE /permissions/groups`  
__Summary__: Remove a Group from a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [GroupPermission](#definitions_GroupPermission)
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content

### <a name="paths_positions"></a> positions  
#### <a name="paths_positions_GET"></a> `GET /positions`  
__Summary__: Fetches a list of Positions  
__Description__: Without any params, it returns a list of last known positions for all the user's Devices. _from_ and _to_ fields are not required with _id_  
##### Parameters  
Name | ParamType | Description | Required | DataType
--- | --- | --- | --- | ---
_deviceId_ | query | _deviceId_ is optional, but requires the _from_ and _to_ parameters when used | False | _integer_
_from_ | query |  | False | _string (date-time)_
_to_ | query |  | False | _string (date-time)_
_id_ | query | To fetch one or more positions. Multiple params can be passed like `id=31&id=42` | False | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Position](#definitions_Position) ]

### <a name="paths_reports"></a> reports  
#### <a name="paths_reportssummary_GET"></a> `GET /reports/summary`  
__Summary__: Fetch a list of ReportSummary within the time period for the Devices or Groups  
__Description__: At least one _deviceId_ or one _groupId_ must be passed  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_deviceId_ | query |  | [ _integer_ ]
_groupId_ | query |  | [ _integer_ ]
_from_ | query | __True__ | _string (date-time)_
_to_ | query | __True__ | _string (date-time)_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [ReportSummary](#definitions_ReportSummary) ]
#### <a name="paths_reportsevents_GET"></a> `GET /reports/events`  
__Summary__: Fetch a list of Events within the time period for the Devices or Groups  
__Description__: At least one _deviceId_ or one _groupId_ must be passed  
##### Parameters  
Name | ParamType | Description | Required | DataType
--- | --- | --- | --- | ---
_deviceId_ | query |  |  | [ _integer_ ]
_groupId_ | query |  |  | [ _integer_ ]
_type_ | query | % can be used to return events of all types |  | [ _string_ ]
_from_ | query |  | __True__ | _string (date-time)_
_to_ | query |  | __True__ | _string (date-time)_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Event](#definitions_Event) ]
#### <a name="paths_reportsroute_GET"></a> `GET /reports/route`  
__Summary__: Fetch a list of Positions within the time period for the Devices or Groups  
__Description__: At least one _deviceId_ or one _groupId_ must be passed  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_deviceId_ | query |  | [ _integer_ ]
_groupId_ | query |  | [ _integer_ ]
_from_ | query | __True__ | _string (date-time)_
_to_ | query | __True__ | _string (date-time)_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Position](#definitions_Position) ]
#### <a name="paths_reportstrips_GET"></a> `GET /reports/trips`  
__Summary__: Fetch a list of ReportTrips within the time period for the Devices or Groups  
__Description__: At least one _deviceId_ or one _groupId_ must be passed  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_deviceId_ | query |  | [ _integer_ ]
_groupId_ | query |  | [ _integer_ ]
_from_ | query | __True__ | _string (date-time)_
_to_ | query | __True__ | _string (date-time)_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [ReportTrips](#definitions_ReportTrips) ]

### <a name="paths_server"></a> server  
#### <a name="paths_server_PUT"></a> `PUT /server`  
__Summary__: Update Server information  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [Server](#definitions_Server)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Server](#definitions_Server)
#### <a name="paths_server_GET"></a> `GET /server`  
__Summary__: Fetch Server information  
##### Parameters  
_None_  
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Server](#definitions_Server)

### <a name="paths_session"></a> session  
#### <a name="paths_session_POST"></a> `POST /session`  
__Summary__: Create a new Session  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_email_ | formData | __True__ | _string_
_password_ | formData | __True__ | _string (password)_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [User](#definitions_User)
_401_ | Unauthorized | 
#### <a name="paths_session_DELETE"></a> `DELETE /session`  
__Summary__: Close the Session  
##### Parameters  
_None_  
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_session_GET"></a> `GET /session`  
__Summary__: Fetch Session information  
##### Parameters  
_None_  
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [User](#definitions_User)
_404_ | Not Found | 

### <a name="paths_statistics"></a> statistics  
#### <a name="paths_statistics_GET"></a> `GET /statistics`  
__Summary__: Fetch server Statistics  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_from_ | query | __True__ | _string (date-time)_
_to_ | query | __True__ | _string (date-time)_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Statistics](#definitions_Statistics) ]

### <a name="paths_users"></a> users  
#### <a name="paths_usersnotifications_POST"></a> `POST /users/notifications`  
__Summary__: Set or unset a Notification  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [Notification](#definitions_Notification)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [Notification](#definitions_Notification)
#### <a name="paths_usersnotifications_GET"></a> `GET /users/notifications`  
__Summary__: Fetch a list of Notification types  
__Description__: Without params, it returns a list of the user's enabled Notifications  
##### Parameters  
Name | ParamType | Description | DataType
--- | --- | --- | ---
_all_ | query | To fetch a list of all available Notifications | _boolean_
_userId_ | query | Standard users can use this only with their own _userId_ | _integer_
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [Notification](#definitions_Notification) ]
#### <a name="paths_usersid_PUT"></a> `PUT /users/{id}`  
__Summary__: Update a User  
##### Parameters  
Name | ParamType | Required | DataType | Schema
--- | --- | --- | --- | ---
_id_ | path | __True__ | _integer_ | 
_body_ | body | __True__ |  | [User](#definitions_User)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [User](#definitions_User)
#### <a name="paths_usersid_DELETE"></a> `DELETE /users/{id}`  
__Summary__: Delete a User  
##### Parameters  
Name | ParamType | Required | DataType
--- | --- | --- | ---
_id_ | path | __True__ | _integer_
##### Responses  
Status Code | Description
--- | ---
_204_ | No Content
#### <a name="paths_users_POST"></a> `POST /users`  
__Summary__: Create a User  
##### Parameters  
Name | ParamType | Required | Schema
--- | --- | --- | ---
_body_ | body | __True__ | [User](#definitions_User)
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [User](#definitions_User)
#### <a name="paths_users_GET"></a> `GET /users`  
__Summary__: Fetch a list of Users  
##### Parameters  
_None_  
##### Responses  
Status Code | Description | Schema
--- | --- | ---
_200_ | OK | [ [User](#definitions_User) ]
_400_ | No Permission | 


## <a name="definitions"></a> Models  
### <a name="definitions_AttributeAlias"></a>AttributeAlias  
Property | Type
--- | ---
_alias_ | _string_
_attribute_ | _string_
_deviceId_ | _integer_
_id_ | _integer_
### <a name="definitions_Command"></a>Command  
Property | Type
--- | ---
_attributes_ | 
_deviceId_ | _integer_
_type_ | _string_
### <a name="definitions_CommandType"></a>CommandType  
Property | Type
--- | ---
_type_ | _string_
### <a name="definitions_Device"></a>Device  
Property | Type
--- | ---
_attributes_ | 
_category_ | _string_
_contact_ | _string_
_geofenceIds_ | _array_
_groupId_ | _integer_
_id_ | _integer_
_lastUpdate_ | _string (date-time)_
_model_ | _string_
_name_ | _string_
_phone_ | _string_
_positionId_ | _integer_
_status_ | _string_
_uniqueId_ | _string_
### <a name="definitions_DeviceGeofence"></a>DeviceGeofence  
Property | Type
--- | ---
_deviceId_ | _integer_
_geofenceId_ | _integer_
### <a name="definitions_DevicePermission"></a>DevicePermission  
Property | Type
--- | ---
_deviceId_ | _integer_
_userId_ | _integer_
### <a name="definitions_DeviceTotalDistance"></a>DeviceTotalDistance  
Property | Type | Description
--- | --- | ---
_deviceId_ | _integer_ | 
_totalDistance_ | _number_ | in meters
### <a name="definitions_Event"></a>Event  
Property | Type
--- | ---
_attributes_ | 
_deviceId_ | _integer_
_geofenceId_ | _integer_
_id_ | _integer_
_positionId_ | _integer_
_serverTime_ | _string (date-time)_
_type_ | _string_
### <a name="definitions_Geofence"></a>Geofence  
Property | Type
--- | ---
_area_ | _string_
_attributes_ | 
_description_ | _string_
_id_ | _integer_
_name_ | _string_
### <a name="definitions_GeofencePermission"></a>GeofencePermission  
Property | Type
--- | ---
_geofenceId_ | _integer_
_userId_ | _integer_
### <a name="definitions_Group"></a>Group  
Property | Type
--- | ---
_attributes_ | 
_groupId_ | _integer_
_id_ | _integer_
_name_ | _string_
### <a name="definitions_GroupGeofence"></a>GroupGeofence  
Property | Type
--- | ---
_geofenceId_ | _integer_
_groupId_ | _integer_
### <a name="definitions_GroupPermission"></a>GroupPermission  
Property | Type
--- | ---
_groupId_ | _integer_
_userId_ | _integer_
### <a name="definitions_Notification"></a>Notification  
Property | Type
--- | ---
_attributes_ | 
_id_ | _integer_
_type_ | _string_
_userId_ | _integer_
### <a name="definitions_Position"></a>Position  
Property | Type | Description
--- | --- | ---
_address_ | _string_ | 
_altitude_ | _number_ | 
_attributes_ |  | 
_course_ | _number_ | 
_deviceId_ | _integer_ | 
_deviceTime_ | _string (date-time)_ | 
_fixTime_ | _string (date-time)_ | 
_id_ | _integer_ | 
_latitude_ | _number_ | 
_longitude_ | _number_ | 
_outdated_ | _boolean_ | 
_protocol_ | _string_ | 
_serverTime_ | _string (date-time)_ | 
_speed_ | _number_ | In knots
_valid_ | _boolean_ | 
### <a name="definitions_ReportSummary"></a>ReportSummary  
Property | Type | Description
--- | --- | ---
_averageSpeed_ | _number_ | in knots
_deviceId_ | _integer_ | 
_deviceName_ | _string_ | 
_distance_ | _number_ | in meters
_engineHours_ | _integer_ | 
_maxSpeed_ | _number_ | in knots
### <a name="definitions_ReportTrips"></a>ReportTrips  
Property | Type | Description
--- | --- | ---
_averageSpeed_ | _number_ | in knots
_deviceId_ | _integer_ | 
_deviceName_ | _string_ | 
_distance_ | _number_ | in meters
_duration_ | _integer_ | 
_endAddress_ | _string_ | 
_endLat_ | _number_ | 
_endLon_ | _number_ | 
_endTime_ | _string (date-time)_ | 
_maxSpeed_ | _number_ | in knots
_startAddress_ | _string_ | 
_startLat_ | _number_ | 
_startLon_ | _number_ | 
_startTime_ | _string (date-time)_ | 
### <a name="definitions_Server"></a>Server  
Property | Type
--- | ---
_attributes_ | 
_bingKey_ | _string_
_coordinateFormat_ | _string_
_distanceUnit_ | _string_
_forceSettings_ | _boolean_
_id_ | _integer_
_latitude_ | _number_
_longitude_ | _number_
_map_ | _string_
_mapUrl_ | _string_
_readonly_ | _boolean_
_registration_ | _boolean_
_speedUnit_ | _string_
_twelveHourFormat_ | _boolean_
_version_ | _string_
_zoom_ | _integer_
### <a name="definitions_Statistics"></a>Statistics  
Property | Type
--- | ---
_activeDevices_ | _integer_
_activeUsers_ | _integer_
_captureTime_ | _string (date-time)_
_messagesReceived_ | _integer_
_messagesStored_ | _integer_
_requests_ | _integer_
### <a name="definitions_User"></a>User  
Property | Type
--- | ---
_admin_ | _boolean_
_attributes_ | 
_coordinateFormat_ | _string_
_deviceLimit_ | _integer_
_disabled_ | _boolean_
_distanceUnit_ | _string_
_email_ | _string_
_expirationTime_ | _string (date-time)_
_id_ | _integer_
_latitude_ | _number_
_longitude_ | _number_
_map_ | _string_
_name_ | _string_
_password_ | _string_
_readonly_ | _boolean_
_speedUnit_ | _string_
_token_ | _string_
_twelveHourFormat_ | _boolean_
_zoom_ | _integer_


