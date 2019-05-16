package co.ledger.blockchain.graphql.controller

import cats.effect.IO
import co.ledger.blockchain.graphql.service.GraphQLService
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Json}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import sangria.execution._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global

class GraphQLController(graphQLService: GraphQLService) extends HttpController {
  override def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@GET -> Root / "graphql" =>
      for {
        gq <- req.as[GraphQLQuery]
        query = QueryParser.parse(gq.query).get
        result <- IO.fromFuture(IO(Executor.execute(GraphQLController.schema, query, new ProductRepo, operationName = gq.operationName)))
        r <- Ok(result)
      } yield {
        // to avoid fetal warning
        println(graphQLService)
        r
      }
  }
}

case class GraphQLQuery(query: String, operationName: Option[String], variables: Option[Json])

object GraphQLQuery {
  implicit val decoder: Decoder[GraphQLQuery] = deriveDecoder
}

case class Picture(width: Int, height: Int, url: Option[String])

case class Product(id: String, name: String, description: String) extends Identifiable {
  def picture(size: Int): Picture =
    Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
}

trait Identifiable {
  def id: String
}

class ProductRepo {
  private val Products = List(
    Product("1", "Cheesecake", "Tasty"),
    Product("2", "Health Potion", "+50 HP"))

  def product(id: String): Option[Product] =
    Products find (_.id == id)

  def products: List[Product] = Products

}

object GraphQLController {

  implicit val PictureType = ObjectType(
    "Picture",
    "The product picture",
    fields[Unit, Picture](
      Field("width", IntType, resolve = _.value.width),
      Field("height", IntType, resolve = _.value.height),
      Field("url", OptionType(StringType),
        description = Some("Picture CDN URL"),
        resolve = _.value.url)))

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",
    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.id)))

  val ProductType =
    deriveObjectType[Unit, Product](
      Interfaces(IdentifiableType),
      IncludeMethods("picture"))

  val Id = Argument("id", StringType)

  val QueryType = ObjectType("Query",
    fields[ProductRepo, Unit](
      Field("product", OptionType(ProductType),
        description = Some("Returns a product with specific `id`."),
        arguments = Id :: Nil,
        resolve = c => c.ctx.product(c arg Id)),

      Field("products", ListType(ProductType),
        description = Some("Returns a list of all available products."),
        resolve = _.ctx.products)))

  val schema = Schema(QueryType)

}
