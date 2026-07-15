from PIL import Image
import sys

def process(input_path, output_path):
    img = Image.open(input_path).convert("RGB")
    
    # Convert to grayscale and threshold to remove compression artifacts
    gray = img.convert("L")
    bw = gray.point(lambda x: 0 if x < 20 else 255, '1')
    
    bbox = bw.getbbox()
    if bbox:
        cropped = img.crop(bbox)
        
        # Make a massive canvas to ensure it fits adaptive icon safe zone
        new_size = 2048
        bg = Image.new("RGB", (new_size, new_size), (0, 0, 0))
        
        # We want the logo to be about 45% of the total size for a good adaptive icon
        target_dim = int(new_size * 0.45)
        
        c_w, c_h = cropped.size
        ratio = target_dim / float(max(c_w, c_h))
        target_w = int(c_w * ratio)
        target_h = int(c_h * ratio)
        
        resized_logo = cropped.resize((target_w, target_h), Image.Resampling.LANCZOS)
        
        offset = ((new_size - target_w) // 2, (new_size - target_h) // 2)
        bg.paste(resized_logo, offset)
        bg.save(output_path, quality=95)
    else:
        print("Image was completely black?")

process("e:/apk-project/u4me/app/src/main/res/drawable/ic_u4me_logo.jpg", "e:/apk-project/u4me/app/src/main/res/drawable/ic_u4me_logo.jpg")
