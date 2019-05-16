# GraphQL Explorer

Requirements:

Basic:
- standalone server
- one endpoint for GraphQL query
- provide the same schema as Explorer v3 (so that it can be used by current users)
- bitcoin support (query transaction)
- query explorer v3

Plus:
- ethereum support
- be able to connect to third party explorers

Technical plans:
- GraphQL for Scala: Sangria
- Http Server: Http4s
- Json library: Circe
- Http Client: Http4s async client

Steps:
1. Bootstrap hello world GraphQL server
1.1. Setup dev environment (test client)
2. Implement query schema
3. bitcoin support
4. ethereum support
5. third party explorer support
