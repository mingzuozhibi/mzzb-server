## /api/session

**Check Session**

```
Request:
GET /api/session
Cookie: SESSION

Response:
{
  "success": bool,   # true if logged else false
  "username": string # exists if success is true
  "roles": string    # exists if success is true
}
```

**Login Session**

```
Request:
POST /api/session
{
	"username": string, # required
	"username": string  # required
}

Response:
Set-Cookie: SESSION  # exists if logged
{
	"success": bool, # true if logged else false
}
```

**Logout Session**

```
Request:
DELETE /api/session
Cookie: SESSION

Response:
{
	"success": bool, # true if logout else false
}

Notice: If no session are passed, the user will not
		be logout even if the return is true

```

## /api/users (templated)

**Get One User**

```
Request:
Cookie: SESSION # role: admin
GET /api/users/{userId}

Response:
When Success
	200 OK
{
	"_links": {...},        # link of this user
	"pid": number,          # user identifier
	"enabled": bool,        # user status
	"username": string,     # user login name
	"registerDate": string, # yyyy-MM-dd HH-mm-ss
	"lastLoggedIn": string, # yyyy-MM-dd HH-mm-ss
}

When Not Found
	404	Not Found
When Not Roles
	403 Forbidden
```

**Get All User**

```
Request:
Cookie: SESSION # role: admin
GET /api/users

Response:
When Success
	200 OK
{
    "_embedded": {
        "users": [
            {...},               # same as get one user
            {...},               # same as above
            ...                  # other users
        ]
    },
    "_links": {
        "profile": {...},        # link of profile
        "search": {...},         # link of search
        "self": {...}            # link of self
    },
    "page": {
        "number": number,        # number of page, zero-base
        "size": number,          # element count of page
        "totalElements": number, # total user count
        "totalPages": number     # total page count
    }
}

When Not Roles
	403 Forbidden
```

**Create User**

```
Request:
POST /api/users
Cookie: SESSION # role: admin
{
	"username": string, # required
	"password": string  # required
}

Response:
When Success
	201 Created
{
	"_links": {...},        # link of this user
	"pid": number,          # user identifier
	"enabled": bool,        # user status
	"username": string,     # user login name
	"registerDate": string, # yyyy-MM-dd HH-mm-ss
	"lastLoggedIn": string, # yyyy-MM-dd HH-mm-ss
}

When Param Error
	400	Bad Request
When Not Roles
	403 Forbidden
When Not Allow
	409	Conflict
```

**Drop User**

```
Request:
DELETE /api/users/{userId}
Cookie: SESSION # role: admin
{
	"username": string, # required
	"username": string  # required
}

Response:
When Success
	204	No Content
When Not Roles
	403 Forbidden
When Not Found
	404	Not Found
```

**Patch User**

```
Request:
PATCH /api/users/{userId}
Cookie: SESSION # role: admin
{
	..., # propperties want modify (include password)
}

Response:
When Success
	200	OK
{
	"_links": {...},        # link of this user
	"pid": number,          # user identifier
	"enabled": bool,        # user status
	"username": string,     # user login name
	"registerDate": string, # yyyy-MM-dd HH-mm-ss
	"lastLoggedIn": string, # yyyy-MM-dd HH-mm-ss
}

When Param Error
	400	Bad Request
When Not Roles
	403 Forbidden
When Not Allow
	409	Conflict
```

**Find By Username**

```
Request:
Cookie: SESSION # role: admin
GET /api/users/search/findByUsername?username={username}

Response:
When Success
	200 OK
{
	"_links": {...},        # link of this user
	"pid": number,          # user identifier
	"enabled": bool,        # user status
	"username": string,     # user login name
	"registerDate": string, # yyyy-MM-dd HH-mm-ss
	"lastLoggedIn": string, # yyyy-MM-dd HH-mm-ss
}

When Not Found
	404	Not Found
When Not Roles
	403 Forbidden
```
