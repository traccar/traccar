# [Traccar Custom](https://www.traccar.org)

## Overview

Traccar is an open source GPS tracking system. This repository contains Java-based back-end service. It supports more than 200 GPS protocols and more than 2000 models of GPS tracking devices. Traccar can be used with any major SQL database system. It also provides easy to use [REST API](https://www.traccar.org/traccar-api/).

**This version is a fork of the original [TRACCAR](https://github.com/traccar/traccar) repository aimed at adding some useful features**

## Features in this version

1. **_`Websocket` can be accessed from external Hosts (App):_**

##### Using _Session ID_

The session ID can be retrieved as cookies from the response on these API endpoints :

- [api/session](https://www.traccar.org/api-reference/#tag/Session/paths/~1session/post) | _Create a new Session_
- [api/session?token=](https://www.traccar.org/api-reference/#tag/Session/paths/~1session/get) | _Fetch Session information_
- [api/devices](https://www.traccar.org/api-reference/#tag/Devices/paths/~1devices/get) | _Fetch a list of Devices_

```js
const socket = new WebSocket(
  "./api/socket?session=node01d8bcd8o4su6u1ug70qdrena0i1"
);
socket.onerror = (error) => {
  console.log("socket error: ", error);
};
socket.onmessage = function (event) {
  console.log("socket message : ", data);
};
```

##### Using user _Access Token_

```js
const socket = new WebSocket(
  "./api/socket?token=SDBGAiEA4SC67Qk5lrCsB2I53EDp5gAR1uips64FRn6W0Dt0jrMCIQDnZ....."
);
socket.onerror = (error) => {
  console.log("socket error: ", error);
};
socket.onmessage = function (event) {
  console.log("socket message : ", data);
};
```

2. **_Add `api/session/check-sid?sid=[SESSION_ID]` to check if the session is still active or not_**

## Related projects

- **[Laravel Traccar package](https://github.com/mr-wolf-gb/traccar)**

## License

    Apache License, Version 2.0

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
