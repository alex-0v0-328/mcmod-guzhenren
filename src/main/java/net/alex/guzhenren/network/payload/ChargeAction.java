package net.alex.guzhenren.network.payload;

/**
 * The three moves of the Store Strength Gu's [蓄力蛊] charge [蓄力] intent: start, cancel, or release at the locked target.
 * Lives with the payload that carries it, so the payload needs nothing from {@code net.alex.guzhenren.network}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum ChargeAction { START, CANCEL, RELEASE }
