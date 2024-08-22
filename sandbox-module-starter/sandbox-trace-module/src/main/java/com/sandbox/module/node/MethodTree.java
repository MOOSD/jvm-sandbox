package com.sandbox.module.node;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class MethodTree {

    // 根节点
    private final MethodNode root;

    // 当前节点
    private MethodNode current;

    public MethodTree(Object title) {
        this.root = new MethodNode(title).markBegin();
        this.current = root;
    }

    public MethodTree updateEnd(){
        current.markEnd();
        root.markEnd();
        return this;
    }

    public boolean isTop() {
        return current.isRoot();
    }

    /**
     * 创建一个分支节点
     *
     * @param data 节点数据
     * @return this
     */
    public MethodTree begin(Object data) {
//        current = new MethodTree.MethodNode(current, data);
//        current.markBegin();

        current = new MethodNode(current, data);
        current.markBegin();

        return this;
    }

    public MethodTree begin() {
        return begin(null);
    }

    public Object get() {
//        if (current.isRoot()) {
//            throw new IllegalStateException("current node is root.");
//        }
        return current.data;
    }

    public MethodTree set(Object data) {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.data = data;
        return this;
    }

    /**
     * 结束一个分支节点
     *
     * @return this
     */
    public MethodTree end() {
        current.markEnd();
        if(!current.isRoot()){
            current = current.parent;
        }
        return this;
    }

    public MethodNode getRoot() {
        return root;
    }

    public MethodNode getCurrent() {
        return current;
    }

    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, MethodNode node);

    }

    public MethodTreeDTO convertToDTO(MethodNode node) {
        MethodTreeDTO dto = new MethodTreeDTO();
        dto.setData(node.data.toString());
        dto.setBeginTimestamp(node.beginTimestamp);
        dto.setEndTimestamp(node.endTimestamp);
        for (MethodNode child : node.children) {
            dto.getChildren().add(convertToDTO(child));
        }
        return dto;
    }

    public String getCurrentMethodNodeStr(){
        return getCurrent().toString();
    }



    private static class MethodNode {

        final MethodNode parent;

        /**
         * 节点数据
         */
        Object data;

        /**
         * 子节点
         */
        final List<MethodNode> children = new ArrayList<>();

        /**
         * 开始时间戳
         */
        private long beginTimestamp;

        /**s
         * 结束时间戳
         */
        private long endTimestamp;

        private MethodNode(Object data) {
            this.parent = null;
            this.data = data;
        }

        private MethodNode(MethodNode parent, Object data) {
            this.parent = parent;
            this.data = data;
            parent.children.add(this);
        }

        /**
         * 是否根节点
         *
         * @return true / false
         */
        boolean isRoot() {
                return null == parent;
            }

        /**
         * 是否叶子节点
         *
         * @return true / false
         */
        boolean isLeaf() {
            return children.isEmpty();
        }

        MethodNode markBegin() {
            beginTimestamp = currentTimeMillis();
            return this;
        }

        MethodNode markEnd() {
            endTimestamp = currentTimeMillis();
            return this;
        }

    }

}
