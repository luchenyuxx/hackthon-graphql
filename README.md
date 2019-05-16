# dSense Gateway

Ledger's entry point to gather and process Oracle's data:
 * Receive data gathered by the Oracles from the firmware gateways
 * Sign and broadcast on EWF the triplet (address, message, signature)
 * Keep a history of the non-sensitive data
 * Expose all the Oracle data except the signature for a 1 month period

## Getting started
 
The application can run either with a H2 or PostgreSQL backend, however, some functionality may not work as expected with H2.
 
Run a PostgreSQL backend on your machine with docker:
```
$ docker run -p 5432:5432 --name postgres postgres:10
``` 

Connect to your PostgreSQL instance and create the database and user for dSense:
```
$ psql -h localhost -p 5432 -U postgres
> CREATE DATABASE dsense;
> CREATE USER sa;
```

Start the application:
```
$ sbt run
```

Note: By default in dev configuration, the database is cleaned up at application startup. You can change this behavior in `src/main/resources/application.conf`

Run unit tests:

```
$ sbt test
```

Run integration tests:
```
$ sbt it:test
```


## API

### Technical endpoints

#### `GET /_health`

```
Response: {
    "status": string
}
```

Perform an healthcheck. Return `200 Ok` if the application is running.

Return:
 * `status`: `Ok` if the application is healthy.

#### `GET /_version`

```
Response: {
    "version": string,
    "sha1": string
}
```

Get the running application version.

Return:
 * `version`: Current version number, `x.x.x` format.
 * `sha1`: Hash of the version commit.
 
#### `GET /_metrics`
 
Expose Prometheus metrics.
 
### Private endpoints
 
This endpoints have protected access.
 
#### `GET /private/cache/status`
 
```
Response: {
    "pending": number
}
```

Return:
 * `pending`: The number of pending entries in the entry cache.

#### `GET /private/cache/synchronize`

```
Response: {
    "sent": number,
    "remaining": number 
}
```

Return:
 * `sent`: The number of pending entries sent.
 * `remaining`: The number of entries still pending.
 
#### `GET /private/cache/purge`

Remove all entries from cache older than 1 month.

### Public endpoints

#### `POST /gateway/{clientId}/entry`

```
Payload: {
    "oracleSignature": string,
    "nodeId": number,
    "oracleCounter": number,
    "oracleTicks": number
}
```

Endpoint called by the firmware gateways to feed the dSense platform.

Input:
 * `oracleSignature`: Hex string of the 65 bytes Oracle's signature.
 * `nodeId`: 64 bits, unique ID of the Oracle's secured element.
 * `oracleCounter`: 64 bits, Oracle's data report counter.
 * `oracleTicks`: 32 bits, number of ticks since the last Oracle's report. 

#### `GET /gateway/{clientId}/entries?startDate={timestamp}&endDate={timestamp}`

```
Response: {
    "clientId": string,
    "from": timestamp,
    "to": timestamp,
    "entries": [{
        "entryDate": timestamp,
        "oracleAddress": string,
        "messageHash": string,
        "nodeId": number,
        "oracleCounter": number,
        "oracleTicks": number 
    }, ...]
}
```

Fetch Oracle data from entry cache. Data is kept available for 1 month. If no date filter is specified, it will return the last 24 hours of entries.

Using the Oracle's address and message hash of each entry, the client can fetch the Oracle's signature from the blockchain and verify the validity of the counter and ticks values. 

Input:
 * `startDate`: Optional date filter. By default, take the last 24 hours before the end date.
 * `endDate`: Optional date filter. By default, take the current date.
 
Return:
 * `clientId`: The client ID used for this request.
 * `from` - `to`: Time range of this request, based on input filter.
 * `entries`: Array of matching entries. 
   * `entryDate`: Entry reception date in dSense.
   * `oracleAddress`: Oracle's address computed by dSense.
   * `messageHash`: Unique message hash computed by dSense.
   * `nodeId`: 64 bits, unique ID of the Oracle's secured element.
   * `oracleCounter`: 64 bits, Oracle's data report counter.
   * `oracleTicks`: 32 bits, number of ticks since the last Oracle's report.
   
## EWF smart contract

Smart contract used for dev purpose is deployed on Tobalaba and can be set in `src/main/resources/application.conf`.

The smart contract exposes 2 functions:

 * `setData(address oracleAddress, bytes32 messageHash, bytes memory oracleSignature) public returns (bool)`: Used by dSense to store the Oracle's signature on the blockchain.
 * `getSignature(address oracleAddress, bytes32 hash) public view returns (bytes memory)`: Used by the end user to fetch the Oracle's signature.
 
## Deployment

The `develop` branch is on continuous deployment to the dev environment located at `dsense-service.dev.aws.ledger.fr:8000`.

The production version is deployed manually from tags on the `master` branch and is located at `dsense-service.prod.aws.ledger.fr:8000`.