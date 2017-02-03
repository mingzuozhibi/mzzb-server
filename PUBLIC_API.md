## Authority

```
Need Role Admin:
1. Path: /api/users
   Method: ALL

No Role Require:
1. Path: /api/**
   Method: ^(GET|HEAD|TRACE|OPTIONS)$
2. Path: /api/session
   Method: ALL

Need Role Basic:
1. Path: /api/**
   Method: ^(POST|PATCH|PUT|DELETE)$
```

## Csrf Token

```
Need Csrf Token:
1. Path: /api/**
   Method: ^(POST|PATCH|PUT|DELETE)$

How To Get Token:
1. Access GET /api/session
2. Parse Response Header
3. Save X-CSRF-HEADER, X-CSRF-PARAM, X-CSRF-TOKEN

How To Use Token:
1. Access POST /api/session (With Json Data)
2. Set Header: ${X-CSRF-HEADER}: ${X-CSRF-TOKEN}
3. If Login Success, You Can Get Session Cookie
4. Or Else You Also Pass Token With Param
5. Add Param Like ${X-CSRF-PARAM}=${X-CSRF-TOKEN}
```

## Cookie

```
1. SESSION
   In order to maintain the login status, check role
   You must pass SESSION with cookie if need any role
```

## /api/session

**Check Session**

```
Need Role: No
Need Csrf: No

Request:
GET /api/session

Response:
{
  "success": bool,   # true if logged else false
  "username": string # exists if success is true
  "roles": string    # exists if success is true
}
```

**Login Session**

```
Need Role: No
Need Csrf: Required

Request:
POST /api/session
{
	"username": string, # required
	"username": string  # required
}

Response:
{
	"success": bool, # true if logged else false
}
```

**Logout Session**

```
Need Role: No
Need Csrf: Required

Request:
DELETE /api/session

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
Need Role: Admin
Need Csrf: No

Request:
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
When Not Role
	403 Forbidden
```

**Get All User**

```
Need Role: Admin
Need Csrf: No

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

When Not Role
	403 Forbidden
```

**Create User**

```
Need Role: Admin
Need Csrf: Required

Request:
POST /api/users
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
When Not Role
	403 Forbidden
When Not Csrf
	403 Forbidden
When Not Allow
	409	Conflict
```

**Drop User**

```
Need Role: Admin
Need Csrf: Required

Request:
DELETE /api/users/{userId}
{
	"username": string, # required
	"username": string  # required
}

Response:
When Success
	204	No Content
When Not Role
	403 Forbidden
When Not Csrf
	403 Forbidden
When Not Found
	404	Not Found
```

**Patch User**

```
Need Role: Admin
Need Csrf: Required

Request:
PATCH /api/users/{userId}
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
When Not Role
	403 Forbidden
When Not Csrf
	403 Forbidden
When Not Allow
	409	Conflict
```

**Find By Username**

```
Need Role: Admin
Need Csrf: No

Request:
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
