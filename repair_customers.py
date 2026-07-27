import re

file_path = "/app/applet/app/src/main/java/com/example/ui/customer/CustomersScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Fix the stray code left behind by bad TextField replacement:
# The regex was `TextField( ... )`, but left behind things like `, singleLine = true )`
# Let's fix this by finding `PremiumTextField(...)` followed by stray `)` or `, singleLine = true )` etc.

# Let's just fix it by replacing the stray parts.
content = re.sub(r'modifier = Modifier\.fillMaxWidth\(\)\n\),\n\s*singleLine = true\n\s*\)', 'modifier = Modifier.fillMaxWidth()\n)', content)
content = re.sub(r'modifier = Modifier\.fillMaxWidth\(\)\n\)\s*,\s*singleLine = true\s*\)', 'modifier = Modifier.fillMaxWidth()\n)', content)

# I can also just manually fix the specific lines.
# But let's check what else is broken.
# "No parameter with name 'alternateNumber' found." in CustomerEntity instantiation.
content = content.replace('alternateNumber = altMobile.trim(),', 'alternateNumber = altMobile.trim(),') # Wait, maybe CustomerEntity doesn't have alternateNumber?
# "Unresolved reference 'altMobile'."
# Because the edit in Customer Form dialog might have removed altMobile?
# Wait! In the Duplicate Dialog, my manual replacement inside fix_customer_full_dialog.py had:
# alternateNumber = altMobile.trim()
# But the original code did NOT have this! I added it because I copied it from MasterDataScreen or somewhere else!
# Let's fix the instantiation in fix_customer_full_dialog block!

content = re.sub(r'alternateNumber = altMobile\.trim\(\),\n\s*', '', content)
content = re.sub(r'address = address1\.trim\(\),', 'address = address.trim(),', content)
content = re.sub(r'siteLocation = siteAddress\.trim\(\),', 'siteLocation = siteLoc.trim(),', content)
content = re.sub(r'whatsappNumber = whatsapp\.trim\(\),\n\s*', '', content) # wait I didn't add whatsapp in my snippet, but I did add it to something?

with open(file_path, "w") as f:
    f.write(content)
