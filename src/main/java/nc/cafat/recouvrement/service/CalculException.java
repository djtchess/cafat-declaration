package nc.cafat.recouvrement.service;

// Exception vérifiée volontaire : rend visible le sujet rollbackFor.
public class CalculException extends Exception {
    public CalculException(String message) { super(message); }
}
