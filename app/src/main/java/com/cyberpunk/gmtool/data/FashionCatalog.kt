package com.cyberpunk.gmtool.data

data class FashionStyle(
    val name: String,
    val keywords: String,
    val prices: Map<String, Int>
)

object FashionCatalog {
    val pieceOrder = listOf("Bottoms", "Top", "Jacket", "Footwear", "Jewelry", "Mirrorshades", "Glasses", "Contact Lenses", "Hats")

    val styles = listOf(
        FashionStyle("Bag Lady Chic", "Homeless • Ragged • Vagrant", mapOf("Bottoms" to 20, "Top" to 10, "Jacket" to 20, "Footwear" to 20, "Jewelry" to 20, "Mirrorshades" to 20, "Glasses" to 10, "Contact Lenses" to 10, "Hats" to 10)),
        FashionStyle("Gang Colors", "Dangerous • Violent • Rebellious", mapOf("Bottoms" to 50, "Top" to 20, "Jacket" to 50, "Footwear" to 20, "Jewelry" to 50, "Mirrorshades" to 20, "Glasses" to 20, "Contact Lenses" to 10, "Hats" to 10)),
        FashionStyle("Generic Chic", "Standard • Colorful • Modular", mapOf("Bottoms" to 50, "Top" to 20, "Jacket" to 50, "Footwear" to 20, "Jewelry" to 50, "Mirrorshades" to 20, "Glasses" to 20, "Contact Lenses" to 10, "Hats" to 10)),
        FashionStyle("Bohemian", "Folksy • Retro • Free-Spirited", mapOf("Bottoms" to 50, "Top" to 20, "Jacket" to 50, "Footwear" to 50, "Jewelry" to 100, "Mirrorshades" to 50, "Glasses" to 50, "Contact Lenses" to 10, "Hats" to 10)),
        FashionStyle("Leisurewear", "Comfort • Agility • Athleticism", mapOf("Bottoms" to 100, "Top" to 20, "Jacket" to 100, "Footwear" to 50, "Jewelry" to 100, "Mirrorshades" to 50, "Glasses" to 50, "Contact Lenses" to 20, "Hats" to 50)),
        FashionStyle("Nomad Leathers", "Western • Rugged • Tribal", mapOf("Bottoms" to 100, "Top" to 20, "Jacket" to 100, "Footwear" to 100, "Jewelry" to 100, "Mirrorshades" to 50, "Glasses" to 50, "Contact Lenses" to 20, "Hats" to 100)),
        FashionStyle("Asia Pop", "Bright • Costume-like • Youthful", pieceOrder.associateWith { 100 }),
        FashionStyle("Urban Flash", "Flashy • Technological • Streetwear", mapOf("Bottoms" to 100, "Top" to 20, "Jacket" to 100, "Footwear" to 100, "Jewelry" to 100, "Mirrorshades" to 100, "Glasses" to 100, "Contact Lenses" to 100, "Hats" to 100)),
        FashionStyle("Businesswear", "Leadership • Presence • Authority", mapOf("Bottoms" to 500, "Top" to 50, "Jacket" to 500, "Footwear" to 500, "Jewelry" to 5000, "Mirrorshades" to 500, "Glasses" to 500, "Contact Lenses" to 100, "Hats" to 500)),
        FashionStyle("High Fashion", "Exclusive • Designer • Couture", mapOf("Bottoms" to 1000, "Top" to 500, "Jacket" to 1000, "Footwear" to 5000, "Jewelry" to 50000, "Mirrorshades" to 1000, "Glasses" to 1000, "Contact Lenses" to 1000, "Hats" to 5000))
    )

    val storeItems: List<StoreItem> = styles.flatMap { style ->
        pieceOrder.map { piece ->
            StoreItem(
                name = "${style.name} — $piece",
                basePrice = style.prices.getValue(piece),
                category = "Clothing",
                description = "یک قطعه $piece از استایل ${style.name}. این آیتم زره نیست و SP ایجاد نمی‌کند؛ برای Wardrobe & Style و نمایش جایگاه/فرهنگ کاراکتر استفاده می‌شود.",
                subtitle = "${style.name} • $piece • ${style.keywords}"
            )
        }
    }
}
