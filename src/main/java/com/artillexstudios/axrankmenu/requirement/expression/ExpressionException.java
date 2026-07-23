package com.artillexstudios.axrankmenu.requirement.expression;

public final class ExpressionException extends IllegalArgumentException {
    private final int position;

    public ExpressionException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }

    public int position() {
        return position;
    }
}
