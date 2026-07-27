perl -0777 -pi -e 's/interface CompanyProfileDao/\@Dao\ninterface CompanyProfileDao/g' app/src/main/java/com/example/data/Daos.kt
perl -0777 -pi -e 's/interface CustomerDao/\@Dao\ninterface CustomerDao/g' app/src/main/java/com/example/data/Daos.kt
perl -0777 -pi -e 's/interface QuotationTemplateDao/\@Dao\ninterface QuotationTemplateDao/g' app/src/main/java/com/example/data/Daos.kt
perl -0777 -pi -e 's/interface QuotationDao/\@Dao\ninterface QuotationDao/g' app/src/main/java/com/example/data/Daos.kt
perl -0777 -pi -e 's/interface QuotationItemDao/\@Dao\ninterface QuotationItemDao/g' app/src/main/java/com/example/data/Daos.kt
perl -0777 -pi -e 's/interface MasterDao/\@Dao\ninterface MasterDao/g' app/src/main/java/com/example/data/Daos.kt
