package com.IT_market.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;

    // Constructor - Spring injects the configured Cloudinary bean
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadProductImage(MultipartFile file) throws IOException {
        // No temporary file needed! Upload directly from bytes.
        Map uploadResult = cloudinary.uploader().upload(
            file.getBytes(), // Pass the byte array directly
            ObjectUtils.asMap(
                "folder", "IT_market/products",
                "resource_type", "auto", // Auto-detect image/video
                "public_id", null, // Let Cloudinary generate a unique ID
                "overwrite", false, // Don't overwrite existing files
                "unique_filename", true // Ensure filename is unique
            )
        );

        // Return the secure HTTPS URL from Cloudinary's response
        return (String) uploadResult.get("secure_url");
    }

    // Optional: Method to delete an image from Cloudinary
    public void deleteImage(String imageUrl) throws IOException {
        // Extract the public_id from the URL
        // Example URL: https://res.cloudinary.com/your-cloud/image/upload/v1234567/IT_market/products/abc123.jpg
        // Public ID would be: "IT_market/products/abc123"
        String publicId = extractPublicIdFromUrl(imageUrl);
        if (publicId != null) {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // This extracts the folder and filename without extension
            String[] parts = url.split("/");
            // Find the index after 'upload' segment
            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i])) {
                    uploadIndex = i;
                    break;
                }
            }
            
            if (uploadIndex >= 0 && uploadIndex + 1 < parts.length) {
                // Join parts after 'upload', skipping version part (starts with 'v')
                StringBuilder publicId = new StringBuilder();
                for (int i = uploadIndex + 1; i < parts.length; i++) {
                    // Skip the version string (e.g., "v1234567")
                    if (parts[i].startsWith("v") && parts[i].length() > 1 && 
                        parts[i].substring(1).matches("\\d+")) {
                        continue;
                    }
                    if (publicId.length() > 0) publicId.append("/");
                    // Remove file extension
                    String part = parts[i];
                    int dotIndex = part.lastIndexOf('.');
                    if (dotIndex > 0) {
                        part = part.substring(0, dotIndex);
                    }
                    publicId.append(part);
                }
                return publicId.toString();
            }
        } catch (Exception e) {
            System.err.println("Error extracting public ID from URL: " + url);
        }
        return null;
    }
}