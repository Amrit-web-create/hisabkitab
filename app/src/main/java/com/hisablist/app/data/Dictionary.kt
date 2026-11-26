package com.hisablist.app.data

import java.util.Locale

/** Text normalisation shared by the item dictionary and the voice parser. */
object Text {
    private val keep = Regex("[^\\p{L}\\p{M}\\p{N}.,]")
    private val dotFix = Regex("(?<!\\d)\\.|\\.(?!\\d)")
    private val digitLetter = Regex("(?<=\\d)(?=[\\p{L}\\p{M}])|(?<=[\\p{L}\\p{M}])(?=\\d)")
    private val spaces = Regex("\\s+")

    fun tokens(raw: String): List<String> {
        var s = raw.lowercase(Locale.ROOT).replace("₹", " rs ")
        s = s.replace(keep, " ")
        s = s.replace(dotFix, " ")
        s = s.replace(",", " , ")
        s = s.replace(digitLetter, " ")
        return s.split(spaces).filter { it.isNotEmpty() }
    }

    fun asciiDigits(tok: String): String {
        val sb = StringBuilder()
        for (c in tok) {
            if (Character.isDigit(c)) sb.append(Character.digit(c, 10)) else sb.append(c)
        }
        return sb.toString()
    }

    fun lev(a: String, b: String): Int {
        if (a == b) return 0
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return dp[b.length]
    }
}

/** Words the parser understands besides item names. */
object Lex {
    val numbers: Map<String, Double> = mapOf(
        "zero" to 0.0, "shunya" to 0.0,
        "ek" to 1.0, "one" to 1.0, "एक" to 1.0,
        "two" to 2.0, "do" to 2.0, "दो" to 2.0,
        "three" to 3.0, "teen" to 3.0, "tin" to 3.0, "तीन" to 3.0,
        "four" to 4.0, "char" to 4.0, "chaar" to 4.0, "चार" to 4.0,
        "five" to 5.0, "paanch" to 5.0, "panch" to 5.0, "pach" to 5.0, "पांच" to 5.0, "पाँच" to 5.0,
        "six" to 6.0, "chhe" to 6.0, "chhah" to 6.0, "chah" to 6.0, "छह" to 6.0, "छे" to 6.0, "छः" to 6.0,
        "seven" to 7.0, "saat" to 7.0, "सात" to 7.0,
        "eight" to 8.0, "aath" to 8.0, "ath" to 8.0, "आठ" to 8.0,
        "nine" to 9.0, "nau" to 9.0, "नौ" to 9.0,
        "ten" to 10.0, "das" to 10.0, "दस" to 10.0,
        "eleven" to 11.0, "gyarah" to 11.0, "ग्यारह" to 11.0,
        "twelve" to 12.0, "barah" to 12.0, "बारह" to 12.0,
        "thirteen" to 13.0, "terah" to 13.0, "तेरह" to 13.0,
        "fourteen" to 14.0, "chaudah" to 14.0, "चौदह" to 14.0,
        "fifteen" to 15.0, "pandrah" to 15.0, "पंद्रह" to 15.0,
        "sixteen" to 16.0, "solah" to 16.0, "सोलह" to 16.0,
        "seventeen" to 17.0, "satrah" to 17.0, "सत्रह" to 17.0,
        "eighteen" to 18.0, "atharah" to 18.0, "अठारह" to 18.0,
        "nineteen" to 19.0, "unnis" to 19.0, "उन्नीस" to 19.0,
        "twenty" to 20.0, "bees" to 20.0, "बीस" to 20.0,
        "thirty" to 30.0, "tees" to 30.0, "तीस" to 30.0,
        "forty" to 40.0, "chalis" to 40.0, "चालीस" to 40.0,
        "fifty" to 50.0, "pachas" to 50.0, "पचास" to 50.0,
        "sixty" to 60.0, "saath" to 60.0, "साठ" to 60.0,
        "seventy" to 70.0, "sattar" to 70.0, "सत्तर" to 70.0,
        "eighty" to 80.0, "assi" to 80.0, "अस्सी" to 80.0,
        "ninety" to 90.0, "nabbe" to 90.0, "नब्बे" to 90.0,
        "half" to 0.5, "aadha" to 0.5, "adha" to 0.5, "aadhaa" to 0.5, "आधा" to 0.5, "आधी" to 0.5,
        "dedh" to 1.5, "derh" to 1.5, "डेढ़" to 1.5, "डेढ" to 1.5,
        "dhai" to 2.5, "dhaai" to 2.5, "ढाई" to 2.5,
        "sava" to 1.25, "savaa" to 1.25, "सवा" to 1.25,
        "pauna" to 0.75, "पौन" to 0.75
    )

    val hundred = setOf("sau", "sao", "hundred", "सौ")

    val units: Map<String, String> = buildMap {
        fun u(unit: String, words: String) { for (w in words.split(",")) put(w.trim(), unit) }
        u("kg", "kg,kgs,kilo,kilos,kilogram,kilograms,kilogramme,किलो,किलोग्राम,केजी")
        u("g", "g,gm,gms,gram,grams,gramme,ग्राम,ग्रा")
        u("l", "l,lt,ltr,litre,litres,liter,liters,लीटर,लिटर,ली")
        u("ml", "ml,mls,milliliter,millilitre,मिली,मिलीलीटर")
        u("pc", "pc,pcs,piece,pieces,nag,nang,dana,dane,नग,पीस,दाना")
        u("packet", "packet,packets,pkt,pack,packs,पैकेट,पैक")
        u("dozen", "dozen,darjan,darjen,दर्जन")
        u("paav", "paav,pav,paao,पाव")
    }

    val rupee = setOf("rupaye", "rupaya", "rupees", "rupee", "rupay", "rs", "ru", "रुपये", "रुपया", "रुपए", "रूपये", "रुपय")

    val filler = setOf(
        "per", "ka", "ke", "ki", "ko", "rate", "bhav", "bhaav", "price", "add", "chahiye", "chahie",
        "lena", "lana", "dena", "please", "mujhe", "hai", "hain", "se", "me", "mein", "list", "bill",
        "for", "of", "the", "a", "an", "to", "bhi", "wala", "wali", "i", "need", "want",
        "का", "के", "की", "को", "रेट", "भाव", "चाहिए", "लेना", "लाना", "मुझे", "है", "हैं", "से", "में",
        "लिस्ट", "बिल", "प्रति", "जोड़ो", "जोड़", "ऐड", "भी", "वाला", "वाली", "ले", "लो"
    )

    val removeWords = setOf(
        "hatao", "hata", "remove", "delete", "nikalo", "nikal", "cancel", "mitao", "mita",
        "हटाओ", "हटा", "निकालो", "निकाल", "मिटाओ", "मिटा"
    )

    val editWords = setOf("karo", "kar", "badlo", "badal", "change", "set", "update", "करो", "कर", "बदलो", "बदल")

    val afterCommand = setOf("do", "de", "dena", "dijiye", "दो", "दे", "देना", "दीजिए", "दीजिये")

    val separators = setOf(",", ";", "aur", "and", "&", "plus", "fir", "phir", "और", "तथा", "फिर")

    fun number(tok: String): Double? {
        val a = Text.asciiDigits(tok)
        a.toDoubleOrNull()?.let { return it }
        return numbers[tok]
    }
}

data class ItemDef(
    val key: String,
    val en: String,
    val hi: String,
    val unit: String,
    val aliases: List<String>
)

/** Offline bilingual grocery dictionary + phonetic alias map ("allo" -> Potato <-> आलू). */
object Dict {
    private fun i(key: String, en: String, hi: String, unit: String, aliases: String) =
        ItemDef(key, en, hi, unit, aliases.split(",").map { it.trim() }.filter { it.isNotEmpty() })

    val items: List<ItemDef> = listOf(
        // Vegetables
        i("potato", "Potato", "आलू", "kg", "allo,aalu,alu,aloo,aaloo,alo,potato,potatoes,batata,आलू,आलु"),
        i("onion", "Onion", "प्याज", "kg", "pyaj,pyaaj,pyaz,pyaaz,piyaj,onion,onions,kanda,प्याज,प्याज़"),
        i("tomato", "Tomato", "टमाटर", "kg", "tamatar,tamater,tomato,tomatoes,टमाटर"),
        i("garlic", "Garlic", "लहसुन", "kg", "lahsun,lehsun,lasun,garlic,लहसुन"),
        i("ginger", "Ginger", "अदरक", "kg", "adrak,adarak,ginger,अदरक"),
        i("green_chilli", "Green Chilli", "हरी मिर्च", "kg", "hari mirch,hari mirchi,mirchi,mirch,green chilli,green chili,हरी मिर्च,मिर्ची"),
        i("red_chilli", "Red Chilli Powder", "लाल मिर्च", "kg", "lal mirch,lal mirchi,red chilli,red chili,chilli powder,लाल मिर्च"),
        i("coriander", "Coriander", "धनिया", "kg", "dhaniya,dhania,coriander,hara dhaniya,धनिया"),
        i("lemon", "Lemon", "नींबू", "pc", "nimbu,nibu,neembu,lemon,lemons,नींबू,निम्बू"),
        i("cabbage", "Cabbage", "पत्ता गोभी", "kg", "patta gobhi,band gobhi,patta gobi,cabbage,पत्ता गोभी,बंद गोभी"),
        i("cauliflower", "Cauliflower", "फूल गोभी", "kg", "phool gobhi,phul gobhi,gobhi,gobi,cauliflower,फूल गोभी,गोभी"),
        i("brinjal", "Brinjal", "बैंगन", "kg", "baingan,baigan,brinjal,eggplant,बैंगन"),
        i("okra", "Okra", "भिंडी", "kg", "bhindi,okra,ladyfinger,lady finger,भिंडी"),
        i("spinach", "Spinach", "पालक", "kg", "palak,spinach,पालक"),
        i("peas", "Green Peas", "मटर", "kg", "matar,mutter,peas,green peas,मटर"),
        i("carrot", "Carrot", "गाजर", "kg", "gajar,gajjar,carrot,carrots,गाजर"),
        i("cucumber", "Cucumber", "खीरा", "kg", "kheera,khira,kakdi,cucumber,खीरा"),
        // Fruits
        i("banana", "Banana", "केला", "dozen", "kela,banana,bananas,केला"),
        i("apple", "Apple", "सेब", "kg", "seb,apple,apples,सेब"),
        i("mango", "Mango", "आम", "kg", "aam,mango,mangoes,आम"),
        // Dairy & staples
        i("milk", "Milk", "दूध", "l", "doodh,dudh,milk,दूध"),
        i("curd", "Curd", "दही", "kg", "dahi,curd,yogurt,yoghurt,दही"),
        i("paneer", "Paneer", "पनीर", "kg", "paneer,panir,पनीर"),
        i("butter", "Butter", "मक्खन", "pc", "makhan,makkhan,butter,मक्खन"),
        i("ghee", "Ghee", "घी", "kg", "ghee,ghi,घी"),
        i("sugar", "Sugar", "चीनी", "kg", "chini,cheeni,sugar,shakkar,shakkar,चीनी,शक्कर"),
        i("salt", "Salt", "नमक", "packet", "namak,salt,नमक"),
        i("rice", "Rice", "चावल", "kg", "chawal,chaval,rice,चावल"),
        i("atta", "Wheat Flour (Atta)", "आटा", "kg", "atta,aata,flour,wheat flour,आटा"),
        i("maida", "Maida", "मैदा", "kg", "maida,मैदा"),
        i("besan", "Gram Flour (Besan)", "बेसन", "kg", "besan,gram flour,बेसन"),
        i("suji", "Semolina (Suji)", "सूजी", "kg", "suji,sooji,rava,semolina,सूजी"),
        i("poha", "Poha", "पोहा", "kg", "poha,pohe,पोहा"),
        i("dal", "Dal", "दाल", "kg", "dal,daal,dall,दाल"),
        i("toor_dal", "Toor Dal", "अरहर दाल", "kg", "toor dal,tur dal,arhar dal,तूर दाल,अरहर दाल"),
        i("moong_dal", "Moong Dal", "मूंग दाल", "kg", "moong dal,mung dal,moong,मूंग दाल"),
        i("chana_dal", "Chana Dal", "चना दाल", "kg", "chana dal,चना दाल"),
        i("masoor_dal", "Masoor Dal", "मसूर दाल", "kg", "masoor dal,masur dal,मसूर दाल"),
        i("chana", "Chickpeas (Chana)", "चना", "kg", "chana,kabuli chana,chickpeas,चना"),
        i("rajma", "Rajma", "राजमा", "kg", "rajma,rajmah,राजमा"),
        i("oil", "Cooking Oil", "तेल", "l", "tel,tail,oil,cooking oil,refined oil,तेल"),
        i("mustard_oil", "Mustard Oil", "सरसों का तेल", "l", "sarson ka tel,sarso ka tel,sarson tel,mustard oil,सरसों का तेल,सरसों तेल"),
        i("tea", "Tea Powder", "चायपत्ती", "kg", "chai patti,chaipatti,chai,tea,tea powder,चायपत्ती,चाय पत्ती,चाय"),
        i("coffee", "Coffee", "कॉफ़ी", "pc", "coffee,kofi,कॉफी,कॉफ़ी"),
        // Spices
        i("turmeric", "Turmeric", "हल्दी", "kg", "haldi,turmeric,हल्दी"),
        i("cumin", "Cumin (Jeera)", "जीरा", "kg", "jeera,zeera,cumin,जीरा"),
        i("mustard_seed", "Mustard Seeds", "राई", "kg", "rai,sarson,mustard seeds,राई,सरसों"),
        i("garam_masala", "Garam Masala", "गरम मसाला", "packet", "garam masala,गरम मसाला"),
        i("masala", "Masala", "मसाला", "packet", "masala,masale,मसाला"),
        // Packaged & household
        i("bread", "Bread", "ब्रेड", "packet", "bread,double roti,ब्रेड,डबल रोटी"),
        i("eggs", "Eggs", "अंडे", "pc", "ande,anda,egg,eggs,अंडे,अंडा"),
        i("biscuit", "Biscuit", "बिस्कुट", "packet", "biscuit,biscuits,biskut,बिस्कुट"),
        i("noodles", "Noodles", "नूडल्स", "packet", "noodles,maggi,नूडल्स,मैगी"),
        i("namkeen", "Namkeen", "नमकीन", "packet", "namkeen,नमकीन"),
        i("soap", "Soap", "साबुन", "pc", "sabun,soap,साबुन"),
        i("detergent", "Detergent", "सर्फ", "packet", "detergent,surf,washing powder,सर्फ,डिटर्जेंट"),
        i("toothpaste", "Toothpaste", "टूथपेस्ट", "pc", "toothpaste,tooth paste,टूथपेस्ट"),
        i("shampoo", "Shampoo", "शैम्पू", "pc", "shampoo,shampu,शैम्पू"),
        i("matchbox", "Matchbox", "माचिस", "pc", "machis,matchbox,match,माचिस"),
        i("agarbatti", "Incense Sticks", "अगरबत्ती", "packet", "agarbatti,agarbati,incense,अगरबत्ती"),
        // Dry fruits & sweets
        i("almond", "Almonds", "बादाम", "kg", "badam,almond,almonds,बादाम"),
        i("cashew", "Cashew", "काजू", "kg", "kaju,cashew,cashews,काजू"),
        i("raisin", "Raisins", "किशमिश", "kg", "kishmish,raisin,raisins,किशमिश"),
        i("jaggery", "Jaggery", "गुड़", "kg", "gud,gur,jaggery,गुड़,गुड")
    )

    private val byKey: Map<String, ItemDef> = items.associateBy { it.key }

    fun find(key: String): ItemDef? = byKey[key]

    val aliasMap: Map<String, ItemDef> by lazy {
        val m = HashMap<String, ItemDef>()
        for (d in items) {
            for (a in d.aliases + d.en + d.hi) {
                val k = Text.tokens(a).joinToString(" ")
                if (k.isNotEmpty() && !m.containsKey(k)) m[k] = d
            }
        }
        m
    }

    private val maxWords: Int by lazy { aliasMap.keys.maxOf { k -> k.count { it == ' ' } + 1 } }

    private val singles: List<Map.Entry<String, ItemDef>> by lazy {
        aliasMap.entries.filter { ' ' !in it.key && it.key.length >= 5 }
    }

    /** Longest alias match starting at index [from]. Returns item + number of tokens used. */
    fun match(t: List<String>, from: Int): Pair<ItemDef, Int>? {
        var n = minOf(maxWords, t.size - from)
        while (n >= 1) {
            val k = t.subList(from, from + n).joinToString(" ")
            val d = aliasMap[k]
            if (d != null) return d to n
            n--
        }
        return null
    }

    /** One-typo tolerance for longer single words (e.g. "tamaatar"). */
    fun fuzzy(tok: String): ItemDef? {
        if (tok.length < 5) return null
        for (e in singles) {
            if (Math.abs(e.key.length - tok.length) <= 1 && Text.lev(tok, e.key) <= 1) return e.value
        }
        return null
    }
}
