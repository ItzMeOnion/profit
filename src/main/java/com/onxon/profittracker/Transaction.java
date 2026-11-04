package com.onxon.profittracker;

/**
 * One payment seen in chat.
 * incoming = money came TO you, otherwise it went OUT from you.
 */
public record Transaction(long time, boolean incoming, double amount, String player, String raw) {}
