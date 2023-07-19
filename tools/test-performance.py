#!/usr/bin/env python3

import asyncio
import random

host = 'localhost'
port = 5027

messageLogin = bytearray.fromhex('000F313233343536373839303132333435')
messageLocation = bytearray.fromhex('000000000000002b080100000140d4e3ec6e000cc661d01674a5e0fffc00000900000004020100f0000242322318000000000100007a04')

devices = 100
period = 1


class AsyncClient(asyncio.Protocol):

    def __init__(self, loop):
        self.loop = loop
        self.buffer = memoryview(messageLogin)

    def connection_made(self, transport):
        self.send_message(transport)

    def send_message(self, transport):
        transport.write(self.buffer)
        self.buffer = memoryview(messageLocation)
        delay = period * (0.9 + 0.2 * random.random())
        self.loop.call_later(delay, self.send_message, transport)

    def data_received(self, data):
        pass

    def connection_lost(self, exc):
        self.loop.stop()


loop = asyncio.get_event_loop()

for i in range(0, devices):
    loop.create_task(loop.create_connection(lambda: AsyncClient(loop), host, port))

loop.run_forever()
loop.close()
