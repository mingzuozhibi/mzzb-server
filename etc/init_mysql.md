### SETUP MYSQL PLUGIN

```text
INSTALL PLUGIN validate_password SONAME 'validate_password.so';
SET GLOBAL validate_password_policy=LOW;
SET GLOBAL validate_password_length=6;
SHOW VARIABLES LIKE 'validate_password%';
```
