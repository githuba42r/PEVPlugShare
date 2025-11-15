package com.a42r.pevplugshare.domain.repository

import com.a42r.pevplugshare.data.db.entity.PhotoEntity

interface PhotoRepository {
    suspend fun insertPhoto(photo: PhotoEntity)
    suspend fun updatePhoto(photo: PhotoEntity)
    suspend fun deletePhoto(photo: PhotoEntity)
    suspend fun getPhotoByFilePath(filePath: String): PhotoEntity?
    suspend fun getPhotosByLocation(locationId: String): List<PhotoEntity>
    suspend fun deletePhotosByLocation(locationId: String)
    suspend fun deletePhotoById(id: String)
    suspend fun deletePhotoByFilePath(filePath: String)
    suspend fun updatePhotoLocationId(oldLocationId: String, newLocationId: String)
    suspend fun updatePhotoFilePath(oldFilePath: String, newFilePath: String)
}
