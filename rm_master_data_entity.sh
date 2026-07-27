sed -i '/@Entity(/,/) / { /tableName = "master_data"/!b; N; N; N; d }' app/src/main/java/com/example/data/Entities.kt
