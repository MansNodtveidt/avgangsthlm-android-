import numpy as np
from PIL import Image

img = Image.open(r"C:\Users\ns\Downloads\AvgångSthlm.png").convert("RGB")
arr = np.array(img)
h, w = arr.shape[:2]
is_content = ~np.all(arr >= 238, axis=2)
row_counts = is_content.sum(axis=1)

print(f"Image: {w}x{h}")
print(f"Rows 680-870 content counts:")
for y in range(680, 870):
    bar = "#" * (row_counts[y] // 20)
    print(f"y={y:4d}: {row_counts[y]:5d}  {bar}")
