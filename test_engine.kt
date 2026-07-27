import com.example.domain.engine.DimensionParserImpl
import com.example.domain.engine.ItemCalculationEngineImpl
import com.example.domain.models.RawItemInput
import com.example.domain.models.UnitType

fun main() {
    val parser = DimensionParserImpl()
    val engine = ItemCalculationEngineImpl(parser)
    val input = RawItemInput(
        itemName = "Kitchen Cabinets",
        description = "Top cabinets",
        material = "Plywood",
        finish = "Laminate",
        width = "0",
        height = "0",
        depth = "0",
        quantity = 6.0,
        unit = "R_FT",
        rate = 1500.0
    )
    val result = engine.calculateItem(input)
    println("Billable Qty: ${result.billableQuantity}")
    println("Amount: ${result.itemAmount}")
}
