sed -i '1154,1156c\
        val sectionSpacing = 14f\
        val totalTermsSectionHeight = 22f + contentHeight\
        val pageCapacity = engine.maxContentY - engine.topMargin\
        if (totalTermsSectionHeight <= pageCapacity) {\
            engine.ensureSpace(totalTermsSectionHeight, reserveHeader = true)\
        } else {\
            val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f\
            engine.ensureSpace(22f + firstTermH + termSpacing, reserveHeader = true)\
        }' app/src/main/java/com/example/pdf/PdfGenerator.kt
