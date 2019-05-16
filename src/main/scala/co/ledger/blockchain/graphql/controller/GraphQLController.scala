package co.ledger.blockchain.graphql.controller

import java.time.Instant

import cats.effect.IO
import cats.syntax.either._
import co.ledger.blockchain.graphql.model.EthSchema.Block
import co.ledger.blockchain.graphql.service.GraphQLService
import co.ledger.blockchain.graphql.utils.crypto._
import co.ledger.blockchain.graphql.utils.future._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Field, _}

import scala.concurrent.ExecutionContext.Implicits.global

class GraphQLController(graphQLService: GraphQLService) extends HttpController {

  case class Query(query: String)

  implicit val queryDecoder: Decoder[Query] = deriveDecoder

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "graphql" =>
      for {
        gq <- req.as[Query]
        query <- QueryParser.parse(gq.query).toEither.liftTo[IO]
        result <- Executor.execute(EthereumQuery.schema, query, EthereumQuery.context).toIO
        r <- Ok(result)
      } yield r
  }

  graphQLService.test() // to avoid fetal warning
}

object EthereumQuery {

  class QueryContext {
    private val cache = List(
      Block("a1".toHex.right.get, 100, Instant.parse("2019-01-01T00:00:00Z"), List.empty),
      Block("a2".toHex.right.get, 200, Instant.parse("2019-01-02T00:00:00Z"), List.empty)
    )
    def block(hash: String): Option[Block] = cache.find(_.hash == hash.toHex.right.get)
    def blocks: List[Block] = cache
  }

  val BlockType = ObjectType(
    "Block",
    "An ethereum block",
    fields[Unit, Block](
      Field("hash", StringType, resolve = _.value.hash.toString),
      Field("height", IntType, resolve = _.value.height),
      Field("time", StringType, resolve = _.value.time.toString),
      Field("transactions", ListType(StringType), resolve = _.value.transactions.map(_.toString))
    )
  )

  val Hash = Argument("hash", StringType)

  val QueryType = ObjectType("Query",
    fields[QueryContext, Unit](
      Field("block", OptionType(BlockType),
        description = Some("Returns a block with specific `hash`."),
        arguments = Hash :: Nil,
        resolve = c => c.ctx.block(c arg Hash)),
      Field("blocks", ListType(BlockType),
        description = Some("Returns a list of all known blocks."),
        resolve = _.ctx.blocks)
    )
  )

  val schema = Schema(QueryType)
  val context = new EthereumQuery.QueryContext
}








// result <- IO.fromFuture(IO(Executor.execute(GraphQLController.schema, query, new ProductRepo, operationName = gq.operationName)))




//class FetchScheme extends ExecutionScheme {
//  override type Result[Ctx, Res] = Ctx => Fetch[IO, Res]
//
//  override def failed[Ctx, Res](error: Throwable): FetchScheme.this.type = ???
//
//  override def onComplete[Ctx, Res](result: FetchScheme.this.type)(op: => Unit)(implicit ec: ExecutionContext): FetchScheme.this.type = ???
//
//  override def flatMapFuture[Ctx, Res, T](future: Future[T])(resultFn: T => FetchScheme.this.type)(implicit ec: ExecutionContext): FetchScheme.this.type = ???
//
//  override def extended: Boolean = ???
//}



//
//case class Picture(width: Int, height: Int, url: Option[String])
//
//case class Product(id: String, name: String, description: String) extends Identifiable {
//  def picture(size: Int): Picture =
//    Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
//}
//
//trait Identifiable {
//  def id: String
//}
//
//class ProductRepo {
//  private val Products = List(
//    Product("1", "Cheesecake", "Tasty"),
//    Product("2", "Health Potion", "+50 HP"))
//
//  def product(id: String): Option[Product] =
//    Products find (_.id == id)
//
//  def products: List[Product] = Products
//
//}
//
//object GraphQLController {
//
//  implicit val PictureType = ObjectType(
//    "Picture",
//    "The product picture",
//    fields[Unit, Picture](
//      Field("width", IntType, resolve = _.value.width),
//      Field("height", IntType, resolve = _.value.height),
//      Field("url", OptionType(StringType),
//        description = Some("Picture CDN URL"),
//        resolve = _.value.url)))
//
//  val IdentifiableType = InterfaceType(
//    "Identifiable",
//    "Entity that can be identified",
//    fields[Unit, Identifiable](
//      Field("id", StringType, resolve = _.value.id)))
//
//  val ProductType =
//    deriveObjectType[Unit, Product](
//      Interfaces(IdentifiableType),
//      IncludeMethods("picture"))
//
//  val Id = Argument("id", StringType)
//
//  val QueryType = ObjectType("Query",
//    fields[ProductRepo, Unit](
//      Field("product", OptionType(ProductType),
//        description = Some("Returns a product with specific `id`."),
//        arguments = Id :: Nil,
//        resolve = c => c.ctx.product(c arg Id)),
//
//      Field("products", ListType(ProductType),
//        description = Some("Returns a list of all available products."),
//        resolve = _.ctx.products)))
//
//  val schema = Schema(QueryType)
//
//}
