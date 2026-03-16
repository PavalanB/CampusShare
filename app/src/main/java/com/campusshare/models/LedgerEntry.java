package com.campusshare.models;

import com.google.firebase.Timestamp;

/**
 * LedgerEntry lives at /users/{userID}/ledger/{partnerID}
 *
 * balance > 0 means the partner OWES this user (this user has lent more)
 * balance < 0 means this user OWES the partner (this user has borrowed more)
 * balance = 0 means they are even
 *
 * Example:
 *   Student A borrows from Student B →
 *     A's ledger at key B: balance = -1  (A owes B)
 *     B's ledger at key A: balance = +1  (B is owed by A)
 *
 *   Next time B wants to borrow from A →
 *     A's ledger at key B: balance = -1  → B gets PRIORITY
 */
public class LedgerEntry {

    private String partnerID;
    private String partnerName;
    private double balance;       // positive = partner owes me, negative = I owe partner
    private int totalTransactions;
    private Timestamp lastUpdated;

    // Required empty constructor for Firestore
    public LedgerEntry() {}

    public LedgerEntry(String partnerID, String partnerName, double balance) {
        this.partnerID         = partnerID;
        this.partnerName       = partnerName;
        this.balance           = balance;
        this.totalTransactions = 1;
        this.lastUpdated       = Timestamp.now();
    }

    public String getPartnerID()        { return partnerID; }
    public String getPartnerName()      { return partnerName; }
    public double getBalance()          { return balance; }
    public int getTotalTransactions()   { return totalTransactions; }
    public Timestamp getLastUpdated()   { return lastUpdated; }

    public void setPartnerID(String v)       { this.partnerID = v; }
    public void setPartnerName(String v)     { this.partnerName = v; }
    public void setBalance(double v)         { this.balance = v; }
    public void setTotalTransactions(int v)  { this.totalTransactions = v; }
    public void setLastUpdated(Timestamp v)  { this.lastUpdated = v; }

    // true = the partner owes this user a favour → partner gets priority next time
    public boolean doesPartnerHavePriority() {
        return balance < 0; // I owe partner → partner gets priority over me
    }
}
