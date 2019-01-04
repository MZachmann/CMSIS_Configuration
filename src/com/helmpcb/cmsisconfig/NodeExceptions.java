/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helmpcb.cmsisconfig;

/**
 *
 * @author Amr Bekhit
 */
class NodeException
        extends Exception {

    private String message = new String();
    
    public NodeException() {
        super();
    }
    
    public NodeException(String message) {
        super(message);
        this.message = message;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return message;
    }
}

class TargetException
        extends Exception {

    private String message = new String();
    
    public TargetException() {
        super();
    }
    
    public TargetException(String message) {
        super(message);
        this.message = message;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return message;
    }
}