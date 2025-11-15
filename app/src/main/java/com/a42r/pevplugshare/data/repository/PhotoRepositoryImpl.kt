package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.db.dao.PhotoDao
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import com.a42r.pevplugshare.domain.repository.PhotoRepository

class PhotoRepositoryImpl(private val photoDao: PhotoDao) : PhotoRepository {
    override suspend fun insertPhoto(photo: PhotoEntity) {
        photoDao.insertPhoto(photo)
    }

    override suspend fun updatePhoto(photo: PhotoEntity) {
        photoDao.insertPhoto(photo) // Room's insert with OnConflictStrategy.REPLACE acts as upsert
    }

    override suspend fun deletePhoto(photo: PhotoEntity) {
        photoDao.deletePhoto(photo)
    }

    override suspend fun getPhotoByFilePath(filePath: String): PhotoEntity? {
        return photoDao.getPhotoByFilePath(filePath)
    }

    override suspend fun getPhotosByLocation(locationId: String): List<PhotoEntity> {
        return photoDao.getPhotosByLocation(locationId)
    }

    override suspend fun deletePhotosByLocation(locationId: String) {
        photoDao.deletePhotosByLocation(locationId)
    }

    override suspend fun deletePhotoById(id: String) {
        photoDao.deletePhotoById(id)
    }

    override suspend fun deletePhotoByFilePath(filePath: String) {
        photoDao.deletePhotoByFilePath(filePath)
    }

    override suspend fun updatePhotoLocationId(oldLocationId: String, newLocationId: String) {
        photoDao.updatePhotoLocationId(oldLocationId, newLocationId)
    }

    override suspend fun updatePhotoFilePath(oldFilePath: String, newFilePath: String) {
        photoDao.updatePhotoFilePath(oldFilePath, newFilePath)
    }
}
