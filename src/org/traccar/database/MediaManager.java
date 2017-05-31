/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import org.jboss.netty.buffer.ChannelBuffer;
import org.traccar.Config;
import org.traccar.helper.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MediaManager {

    private String path;

    public MediaManager(Config config) {
        path = config.getString("media.path");
    }

    private File createFile(String uniqueId, String name) throws IOException {
        Path filePath = Paths.get(path, uniqueId, name);
        Files.createDirectories(filePath.getParent());
        return filePath.toFile();
    }

    public String writeFile(String uniqueId, ChannelBuffer buf, String extension) {
        int size = buf.readableBytes();
        String name = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + "." + extension;
        try (FileOutputStream output = new FileOutputStream(createFile(uniqueId, name));
                FileChannel fileChannel = output.getChannel()) {
            ByteBuffer byteBuffer = buf.toByteBuffer();
            int written = 0;
            while (written < size) {
                written += fileChannel.write(byteBuffer);
            }
            fileChannel.force(false);
            return name;
        } catch (IOException e) {
            Log.warning(e);
        }
        return null;
    }

}
