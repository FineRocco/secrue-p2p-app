# **Secure P2P Messaging Application**

## **Overview**
This Java-based project implements a secure peer-to-peer (P2P) messaging application. Inspired by modern messaging platforms like WhatsApp, Signal, and Telegram, it prioritizes **end-to-end encryption**, **real-time communication**, and **secure data transmission**. The application ensures that only intended recipients can access messages.

---

## **Features**
- **End-to-End Encryption**: Protects messages from interception by encrypting them from sender to recipient.
- **Real-Time Messaging**: Leverages **SSLSocket** or similar protocols for instant message delivery.
- **Peer-to-Peer Architecture**: Eliminates centralized servers, enhancing privacy and reducing vulnerability to external breaches.
- **Long-Term Cloud Storage Encryption**: Encrypts messages for storage across cloud platforms.
- **Redundant Cloud Storage**: Ensures reliability with support for three providers: **AWS S3**, **Firebase**, and **Azure**.
- **Message Searching**: Allow users to search for keywords and find every message in all their conversations where that word was used.
- **Cross-Platform Compatibility**: Operates on Windows, macOS, and Linux.

---

## **Prerequisites**
1. **Java Development Kit (JDK)**: Version 17 or later.
2. **Maven**: For dependency management and build automation.

---

## **Libraries/Dependencies**
The following dependencies are managed via Maven:
- **Bouncy Castle**: Provides cryptographic functions for secure communication.
- **SSLSocket**: Supports encrypted peer-to-peer messaging.
    - **Version**: [Specify version if available].
    - **Encryption Algorithm**: [Specify, e.g., AES-256, RSA-2048].

---

## **Setup Instructions**
### **1. Build the Project**
Use Maven to resolve dependencies and compile the project:
```bash
mvn clean install
```

### **2. Run the Central Server**
Start the central server with the following command:
```bash
java -jar secure-p2p-app/target/CentralServer.jar
```

### **3. Launch the Client**
Start the Secure P2P Messaging App:
```bash
mvn exec:java -f "secure-p2p-app/secure-p2p-app/pom.xml"
```

---

## **Usage Guide**

### **1. Registration**
- Open the application.
- Register using a **username**, **IP address**, and **port**.

### **2. Direct Messaging**
- Search for a peer using their username.
- Send direct messages encrypted for secure transmission.

### **3. Conversations**
- All conversations are securely stored across three cloud providers.
- Search for a conversation with a user from a dropdown list.
- View all messages in the selected conversation and send new encrypted messages.

### **4. Keyword Search**
- Search for a keyword across all conversations.
- Retrieve all messages containing the specified word.

---

## **Notes**
- Messages are encrypted and stored securely across multiple cloud platforms.
- Redundant storage ensures data availability and mitigates the risk of loss.

---

## **Development Team**

Henrique Alípio nº 64452;
Denis Ungureanu nº 56307;
Pedro Marques nº 64857


