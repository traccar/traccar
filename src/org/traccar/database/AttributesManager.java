/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.sql.SQLException;

import org.traccar.model.Attribute;
import org.traccar.model.BaseModel;

public class AttributesManager extends ExtendedObjectManager {

    public AttributesManager(DataManager dataManager) {
        super(dataManager, Attribute.class);
        refreshItems();
        refreshExtendedPermissions();
    }

    @Override
    public void updateItem(BaseModel item) throws SQLException {
        Attribute attribute = (Attribute) item;
        getDataManager().updateObject(attribute);
        Attribute cachedAttribute = (Attribute) getById(item.getId());
        cachedAttribute.setDescription(attribute.getDescription());
        cachedAttribute.setAttribute(attribute.getAttribute());
        cachedAttribute.setExpression(attribute.getExpression());
        cachedAttribute.setType(attribute.getType());
    }

}
