import yaml
import os

# Input and output file paths
ebextensions_file = "./ebextensions/options.config"
output_file = "./ebextensions.sh"

# Check if the input file exists
if not os.path.exists(ebextensions_file):
    print(f"Error: {ebextensions_file} not found!")
    exit(1)

# Load the YAML file
with open(ebextensions_file, "r") as file:
    data = yaml.safe_load(file)

# Extract the setup.sh content
try:
    setup_content = data["files"]["/home/ec2-user/setup.sh"]["content"]
except KeyError:
    print("Error: /home/ec2-user/setup.sh not found in the YAML file.")
    exit(1)

# Write the extracted content to the output file
os.makedirs(os.path.dirname(output_file), exist_ok=True)
with open(output_file, "w") as file:
    file.write(setup_content)

# Make the output file executable
os.chmod(output_file, 0o755)

print(f"setup.sh content successfully written to {output_file}.")
