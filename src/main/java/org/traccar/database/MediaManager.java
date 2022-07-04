/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MediaManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaManager.class);

    private final String path;

    @Inject
    public MediaManager(Config config) {
        this.path = config.getString(Keys.MEDIA_PATH);
    }

    private File createFile(String uniqueId, String name) throws IOException {
        Path filePath = Paths.get(path, uniqueId, name);
        Path directoryPath = filePath.getParent();
        if (directoryPath != null) {
            Files.createDirectories(directoryPath);
        }
        return filePath.toFile();
    }

    public OutputStream createFileStream(String uniqueId, String name, String extension) throws IOException {
        return new FileOutputStream(createFile(uniqueId, name + "." + extension));
    }

    public String writeFile(String uniqueId, ByteBuf buf, String extension) {
        if (path != null) {
            int size = buf.readableBytes();
            String name = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "." + extension;
            try (FileOutputStream output = new FileOutputStream(createFile(uniqueId, name));
                    FileChannel fileChannel = output.getChannel()) {
                    ByteBuffer byteBuffer = buf.nioBuffer();
                int written = 0;
                while (written < size) {
                    written += fileChannel.write(byteBuffer);
                }
                fileChannel.force(false);
                return name;
            } catch (IOException e) {
                LOGGER.warn("Save media file error", e);
            }
        }
        return null;
    }

}
