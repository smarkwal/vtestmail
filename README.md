# junit-mailserver

Fake SMTP, POP3 and IMAP servers for integration tests in Java projects.

## Installation

### Maven
```
<dependency>
    <groupId>net.markwalder</groupId>
    <artifactId>junit-mailserver-core</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>    
```

### Gradle (Kotlin)
```
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

You can also start multiple servers and use the same store for all of them.

# Developers

* Stephan Markwalder - @smarkwal
