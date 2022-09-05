# vtestmail

[![Release](https://img.shields.io/github/v/release/smarkwal/vtestmail?label=Latest)](https://github.com/smarkwal/vtestmail/releases/latest)
[![License](https://img.shields.io/github/license/smarkwal/vtestmail?label=License)](https://github.com/smarkwal/vtestmail/blob/main/LICENSE)
[![Issues](https://img.shields.io/github/issues/smarkwal/vtestmail?label=Issues)](https://github.com/smarkwal/vtestmail/issues)

[![Build](https://github.com/smarkwal/vtestmail/actions/workflows/build.yml/badge.svg)](https://github.com/smarkwal/vtestmail/actions/workflows/build.yml)
[![Tests](https://img.shields.io/sonar/tests/smarkwal_vtestmail/main?label=Tests&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/component_measures?id=smarkwal_vtestmail&metric=test_success_density&selected=smarkwal_vtestmail%3Asrc%2Ftest%2Fjava%2Forg%2Fsession-logout-listener)
[![Coverage](https://img.shields.io/sonar/coverage/smarkwal_vtestmail/main?label=Coverage&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/component_measures?id=smarkwal_vtestmail&metric=coverage&view=list)
[![Quality](https://img.shields.io/sonar/quality_gate/smarkwal_vtestmail/main?label=Quality&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=smarkwal_vtestmail)

Embedded SMTP, POP3, and IMAP servers for integration tests in Java projects.

#### Problem
You have code which is sending and/or receiving emails using SMTP, POP3, or IMAP?
You want to test your code against a "real" mail server in integration tests?

#### Solution
Run embedded in-memory mail servers as part of your integration tests and allow your test code to connect to them.

## Modules

### vtestmail-core

`vtestmail-core` is the core module of the `vtestmail` project.
It contains the implementations of the SMTP, POP3, and IMAP server.
It does not have any dependencies on other libraries.

## Requirements

* Java 11 or greater. Tested with Java 11 - Java 18 in July 2022.

## Installation

### Maven
```xml
<dependency>
    <groupId>net.markwalder</groupId>
    <artifactId>vtestmail-core</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>    
```

### Gradle (Kotlin)
```kotlin
testImplementation("net.markwalder:vtestmail-core:1.0.0")
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

For implicit SSL/TLS where the server only accepts SSL/TLS connections (also known as SMTPS, IMAPS, or POP3S), call `server.setUseSSL(true)` before starting the server:

```java
SmtpServer server = new SmtpServer(store);
server.setUseSSL(true); // enable implicit SSL/TLS
server.start();
// ...
```

Clients can also switch to an SSL/TLS connection at runtime (also known as "opportunistic TLS") by sending the `STARTTLS` (SMTP and IMAP) or `STLS` (POP3) command.

To disable support for opportunistic TLS, disable the required command by calling `server.setCommandEnabled("STARTTLS", false)` or `server.setCommandEnabled("STLS", false)`.

#### Server certificate

The server will use a self-signed certificate for the domain "localhost" with a 2048-bit RSA key, valid until July 31st 2032. 
The certificate and the RSA key pair are loaded from the classpath resource `vtestmail.pfx`.

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

SMTP standards:

* [RFC 5321 - Simple Mail Transfer Protocol](https://datatracker.ietf.org/doc/html/rfc5321)
* [RFC 4954 - SMTP Service Extension for Authentication](https://datatracker.ietf.org/doc/html/rfc4954)
* [RFC 3207 - SMTP Service Extension for Secure SMTP over Transport Layer Security](https://datatracker.ietf.org/doc/html/rfc3207)
* [RFC 3463 - Enhanced Mail System Status Codes](https://datatracker.ietf.org/doc/html/rfc3463)

Supported commands:

* `HELO` and `EHLO`
* `STARTTLS` 
* `AUTH` 
* `VRFY` 
* `MAIL FROM` 
* `RCPT TO` 
* `DATA` 
* `NOOP` 
* `RSET` 
* `QUIT`

### POP3 server

POP3 standards:

* [RFC 1939 - Post Office Protocol - Version 3](https://datatracker.ietf.org/doc/html/rfc1939)
* [RFC 2195 - IMAP/POP AUTHorize Extension for Simple Challenge/Response](https://datatracker.ietf.org/doc/html/rfc2195)
* [RFC 2449 - POP3 Extension Mechanism](https://datatracker.ietf.org/doc/html/rfc2449)
* [RFC 2595 - Using TLS with IMAP, POP3 and ACAP](https://datatracker.ietf.org/doc/html/rfc2595)
* [RFC 5034 - The Post Office Protocol (POP3) Simple Authentication and Security Layer (SASL) Authentication Mechanism](https://datatracker.ietf.org/doc/html/rfc5034)

Supported commands:

* `CAPA`
* `STLS`
* `AUTH`, `APOP`, `USER` and `PASS`
* `STAT`
* `LIST`
* `UIDL`
* `TOP`
* `RETR`
* `DELE`
* `NOOP`
* `RSET`
* `QUIT`

### IMAP server

IMAP standards:

* [RFC 9051 - Internet Message Access Protocol (IMAP) - Version 4rev2](https://datatracker.ietf.org/doc/html/rfc9051)

Supported commands (work in progress):

* `CAPABILITY`
* `STARTTLS`
* `LOGIN` and `AUTHENTICATE`
* `ENABLE`
* `SELECT`, `EXAMINE`, `UNSELECT`, and `CLOSE`
* `CREATE`, `DELETE`, and `RENAME`
* `SUBSCRIBE` and `UNSUBSCRIBE`
* `NAMESPACE` and `LIST`
* `STATUS`
* `SEARCH`
* `FETCH`
* `STORE`
* `APPEND`
* `COPY`
* `MOVE`
* `UID`
* `EXPUNGE`
* `IDLE`
* `NOOP`
* `LOGOUT`

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
