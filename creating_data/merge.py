import pandas as pd
import os

csv_folder = r"E:\project\creating_data\sign_data" 
merged_data = []
for file in os.listdir(csv_folder):
    if file.endswith(".csv"):
        file_path = os.path.join(csv_folder, file)
        df = pd.read_csv(file_path, header=None)
        merged_data.append(df)

merged_df = pd.concat(merged_data, ignore_index=True)
merged_df.to_csv("merged_sign_data.csv", index=False, header=False)
print("merged")
