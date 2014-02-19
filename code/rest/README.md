## Usage

Request OAuth authorization:

```sh
 curl -X POST -vu angular-client:123456 http://localhost:8080/oauth/token -H "Accept: application/json" -d "password=password&username=user1&grant_type=password&scope=read%2Cwrite&client_secret=123456&client_id=angular-client"
```

A successful authorization results in the following response:

```json
{"access_token":"ff16372e-38a7-4e29-88c2-1fb92897f558","token_type":"bearer","expires_in":43199,"scope":"read write"}
```

Use the `access_token` returned in the previous request to make the authorized request to the protected endpoint:

```sh
curl http://localhost:8080/reservations -H "Authorization: Bearer 5fc3b2f8-a23d-4397-a083-5582e679fef8"
```

If the request is successful, you will see the following:

```json
{"id":1,"content":"Hello, World!"}
```