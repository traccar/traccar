/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.model;

import org.traccar.helper.IgnoreOnSerialization;
import org.traccar.helper.PasswordHash;
import org.traccar.helper.PasswordHash.HashingResult;

public class User implements Factory {

    @Override
    public User create() {
        return new User();
    }

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    private String hashedPassword;
    @IgnoreOnSerialization
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    private String salt;
    @IgnoreOnSerialization
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    private boolean readonly;
    
    private boolean admin;
    public boolean getAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    
    private String map;
    
    private String language;
    
    private String distanceUnit;
    
    private String speedUnit;
    
    private double latitude;
    
    private double longitude;
    
    private int zoom;

    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) {
        this.password = password;
        if(this.password != null && !this.password.trim().equals("")) {
            this.hashPassword(password);
        }
    }

    public boolean isPasswordValid(String inputPassword) {
        return PasswordHash.validatePassword(inputPassword.toCharArray(), PasswordHash.PBKDF2_ITERATIONS, this.salt, this.hashedPassword);
    }
    
    public void hashPassword(String password) {
        HashingResult hashingResult = PasswordHash.createHash(password);
        this.hashedPassword = hashingResult.hash;
        this.salt = hashingResult.salt;
    }

}
