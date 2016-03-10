/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.traccar.model.Group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GroupTree {

    private static class GroupNode {

        private Group group;
        private GroupNode parent;
        private Collection<GroupNode> children = new HashSet<>();

        public GroupNode(Group group) {
            this.group = group;
        }

        @Override
        public int hashCode() {
            return (int) group.getId();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GroupNode)) {
                return false;
            }
            return obj == this || group.getId() == ((GroupNode) obj).group.getId();
        }

        public Group getGroup() {
            return group;
        }

        public void setParent(GroupNode parent) {
            this.parent = parent;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        public GroupNode getParent() {
            return parent;
        }

        public Collection<GroupNode> getChildren() {
            return children;
        }

    }

    private final Map<Long, GroupNode> groupMap = new HashMap<>();

    public GroupTree(Collection<Group> groups) {

        for (Group group : groups) {
            groupMap.put(group.getId(), new GroupNode(group));
        }

        for (GroupNode node : groupMap.values()) {
            if (node.getGroup().getGroupId() != 0) {
                node.setParent(groupMap.get(node.getGroup().getGroupId()));
            }
        }

    }

    public Collection<Group> getDescendants(long groupId) {
        Set<GroupNode> results = new HashSet<>();
        getDescendants(results, groupMap.get(groupId));
        Collection<Group> groups = new ArrayList<>();
        for (GroupNode node : results) {
            groups.add(node.getGroup());
        }
        return groups;
    }

    private void getDescendants(Set<GroupNode> results, GroupNode node) {
        for (GroupNode child : node.getChildren()) {
            results.add(child);
            getDescendants(results, child);
        }
    }

}
