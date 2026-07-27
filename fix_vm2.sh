sed -i '/_newQuoteCustomer.value = CustomerEntity(/,/^            )/c\
            _newQuoteCustomer.value = CustomerEntity(\
                customerId = quotation.customerId.toLong(),\
                customerName = quotation.customerName,\
                mobileNumber = quotation.customerPhone,\
                address = quotation.customerAddress,\
                email = "", city = "", state = "", pincode = "",\
                gstin = "", companyName = "", siteAddress = ""\
            )' app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt
