# junit-mailserver

Embedded SMTP, POP3, and IMAP servers for integration tests in Java projects.

#### Problem
You have code which is sending and/or receiving emails using SMTP, POP3, or IMAP?
You want to test your code against a "real" mail server in integration tests?

#### Solution
Run embedded in-memory mail servers as part of your integration tests and allow your test code to connect to them.

## Modules

### junit-mailserver-core

`junit-mailserver-core` is the core module of the `junit-mailserver` project.
It contains the implementations of the SMTP, POP3, and IMAP server.
It does not have any dependencies on other libraries.

## Requirements

* Java 11 or greater. Tested with Java 11 - Java 18 in July 2022.

## Installation

### Maven
```xml
<dependency>
    <groupId>net.markwalder</groupId>
    <artifactId>junit-mailserver-core</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>    
```

### Gradle (Kotlin)
```kotlin
testImplementation("net.markwalder:junit-mailserver-core:1.0.0")
```

## Usage

### Prepare a mailbox store

Create a `MailboxStore` and add mailboxes to save emails received by the SMTP server, and to hold emails for the POP3 or IMAP server.

```java
MailboxStore store = new MailboxStore();
store.createMailbox("alice", "password123", "alice@localhost");
store.createMailbox("bob", "password321", "bob@localhost");
// ...
```

Username and password are used for authentication.
The email address is used to find a mailbox for incoming emails.
You can use any domain name in email addresses.
Emails sent to non-existing mailboxes are silently discarded.

### SMTP server

To start an SMTP server using the given mailbox store, bound to localhost and listening on a free port:

```java
SmtpServer server = new SmtpServer(store);
server.start();
int port = server.getPort();
// ... use server in integration tests ...
server.stop();
```

### POP3 server

To start a POP3 server using the given mailbox store, bound to localhost and listening on a free port:

```java
Pop3Server server = new Pop3Server(store);
server.start();
int port = server.getPort();
// ... use server in integration tests ...
server.stop();
```

### IMAP server

To start a IMAP server using the given mailbox store, bound to localhost and listening on a free port:

```java
ImapServer server = new ImapServer(store);
server.start();
int port = server.getPort();
// ... use server in integration tests ...
server.stop();
```

You can also start multiple servers and use the same store for all of them.

## Examples

Most of the following examples can be used with all mail server types (SMTP, POP3, and IMAP).
For simplicity, the examples only use the SMTP server.

### Run a separate server for every JUnit test method

TODO

### Run a shared server for all tests in a JUnit test class

TODO

### Set a custom server port

By default, all servers ask the operating system for a random free port when they are started.
To use a specific port, call `server.setPort(int port)` before starting the server:

```java
SmtpServer server = new SmtpServer(store);
server.setPort(2525); // run SMTP server on port 2525
server.start();
// ...
```

#### Default ports

TODO

### How to use SSL/TLS

For implicit SSL/TLS where the server only accepts SSL/TLS connections, call `server.setUseSSL(true)` before starting the server:

```java
SmtpServer server = new SmtpServer(store);
server.setUseSSL(true); // enable implicit SSL/TLS
server.start();
// ...
```

Clients can also switch to a SSL/TLS connection at runtime by sending the `STARTTLS` (SMTP and IMAP) or `STLS` (POP3) command.

The server will use a self-signed certificate for the domain "localhost" with an 2048-bit RSA key, valid until July 31st 2032. 
The certificate and the RSA key pair are loaded from the classpath resource `junit-mailserver.pfx`.

#### Select a specific SSL/TLS protocol

By default, the server uses TLS version 1.2 (`TLSv1.2`) for SSL/TLS connections.
To use a different protocol, call `server.setSSLProtocol(String protocol)` before starting the server:

```java
SmtpServer server = new SmtpServer(store);
server.setSSLProtocol("TLSv1.1"); // use TLS version 1.1
server.start();
// ...
```

Supported protocols:

* `TLSv1.3` - TLS version 1.3
* `TLSv1.2` - TLS version 1.2
* `TLSv1.1` - TLS version 1.1
* `TLSv1` - TLS version 1.0
* `SSLv3` - SSL version 3

Note that the availability of a specific protocol also depends on the Java version and runtime.

### Enable or disable authentication types

TODO

### Enable or disable commands

TODO

### Add custom command

TODO

### Add custom authenticator

TODO

### Prepare a mailbox store

Structure: `MailboxStore` -> `Mailbox` -> `MailboxFolder` -> `MailboxMessage`

TODO

#### Load mailbox store from an XML file

TODO

#### Save mailbox store to an XML file

TODO

### Write test assertions

TODO

#### Inspect mailbox store contents

TODO

#### Inspect mail sessions

TODO

## Supported features

### SMTP server

Supported commands:
* `TODO`

### POP3 server

Supported commands:
* `TODO`

### IMAP server

Supported commands:
* `TODO`

## Limitations

### SMTP server

* Only one SMTP client can connect to the SMTP server at a time.
* The format of email addresses and messages is not validated.
* Messages are not queued or relayed to another SMTP server.
* Messages are either delivered to a local mailbox, or silently discarded.

### POP3 server

* Only one POP3 client can connect to the POP3 server at a time.
* The mailbox is not exclusively locked by the server.
* The format of email messages is not validated.

### IMAP server

* Only one IMAP client can connect to the IMAP server at a time.
* Only IMAP4rev2 (RFC 9051) is supported.
* The format of email messages is not validated.

### Common

* It is not possible to enable two or more SSL/TLS protocols.
* It is not possible to enable or disable specific cipher suites. They are automatically selected by the Java runtime.

## Developers

* Stephan Markwalder - @smarkwal
