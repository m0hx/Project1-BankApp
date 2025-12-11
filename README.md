# Project 1 - Bank App

# Trello
[https://trello.com/b/Lk6OxxIR/project-1-bankapp](https://trello.com/b/Lk6OxxIR/project-1-bankapp)

# Technologies Used:
JDK: Oracle OpenJDK 17.0.17  
IDE: IntelliJ IDEA Community 2025.3  

Libraries (Project Structure -> Libraries -> add) :  
**jbcrypt-0.4.jar**  
**pdfbox-3.0.6.jar**  
**pdfbox-io-3.0.6.jar**  
**fontbox-3.0.6.jar**  
**commons-logging-1.3.5.jar**  

[hr]

# Test Accounts
[Banker]
id: 10001
pw: 0000

[Customer1]
id: 10002
pw: 0000

[Customer2]
id: 10003
pw: 0000

[hr]

# User Stories
1. **Access**
   - As a user, I should be able to interact with the bank app using the command-line interface.

2. **Role-based Login**
   - As a user, I want the system to recognize my role (Banker or Customer) so that I can access the appropriate features.

3. **Account Protection**
   - As a user, I want my account to be locked for 1 minute after 3 failed login attempts to prevent bruteforce and unauthorized access.

4. **Password Security**
   - As a customer, I want to set up my own password when I create my account when my account is created and have it encrypted for security.

5. **Account Creation**
   - As a banker, I want to add new customers to the system and create checking or savings accounts for them.

6. **Money Withdrawal**
   - As a customer, I want to withdraw money from my checking or savings accounts.

7. **Money Deposit**
   - As a customer, I want to deposit money into my checking or savings accounts.

8. **Funds Transfer**
   - As a customer, I want to transfer money between my own accounts or to other customers' accounts.

9. **Transaction Limits**
   - As a customer, I want my transactions to respect daily limits based on my debit card type (Platinum, Titanium, or Standard).

10. **Overdraft Protection**
    - As a customer, I want the system to charge a $35 overdraft fee, limit withdrawals to $100 when negative, and deactivate my account after 2 overdrafts.

11. **Account Reactivation**
    - As a customer, I want to reactivate my deactivated account by resolving the negative balance and paying all overdraft fees.

12. **Transaction History**
    - As a customer, I want to view a history of all my transactions with date, type, and post-transaction balance to track my banking activity.

13. **Detailed Account PDF Statement**
    - As a customer, I want to view a pdf file containing detailed account statement showing the total amount in my account and all transactions with date and time.
   

# Implemented Features
- Login functionality (User can login / System recognizes Banker/Customer roles)
- Add New Customer (Banker can add customers, customers can have checking, savings, or both)
- Account Transactions (Withdraw Money / Deposit Money / Transfer Money between own accounts or to other customers)
- Transaction History (View all transactions with date, type, and post-transaction balance)
- File Encryption (All files encrypted with AES256-GCM)
- Password Hashing with jBCrypt (Passwords encrypted when accounts are created)
- Debit Card Types (3 Mastercard types: Platinum, Titanium, Standard)
- Daily Transaction Limits (Enforced based on debit card type)
- Detailed Account Statement (Shows total amount and transactions with date and time)
- Fraud Detection (3 failed attempts = 1 minute lockout)

  
# Bonus
- PDF Bank Statement (PDF generation with account details and transactions)


# Non-Implemented / Unresolved Issues
- Overdraft Protection
- Account Reactivation
- Filtering transactions

  
# Favorite Functions
- Password Encryption with jBCrypt (Bcrypt)
- File Encryption (All files encrypted with AES256-GCM)
- PDF generation

# ERD diagram

# Additional Resources
https://www.mindrot.org/projects/jBCrypt/#download  
https://mvnrepository.com/artifact/org.mindrot/jbcrypt/0.4  
https://www.tutorialspoint.com/pdfbox/index.htm  
https://pdfbox.apache.org/download.html  
https://commons.apache.org/logging/download_logging.cgi (commons-logging-1.3.5.jar / fontbox-3.0.6.jar / pdfbox-3.0.6.jar / pdfbox-io-3.0.6.jar)  
https://www.baeldung.com/java-aes-encryption-decryption  
https://medium.com/@johnvazna/implementing-local-aes-gcm-encryption-and-decryption-in-java-ac1dacaaa409  


