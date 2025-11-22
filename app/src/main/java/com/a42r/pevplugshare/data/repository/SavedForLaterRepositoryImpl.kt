package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.db.dao.SavedForLaterDao
import com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity
import com.a42r.pevplugshare.domain.repository.SavedForLaterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedForLaterRepositoryImpl @Inject constructor(
    private val savedForLaterDao: SavedForLaterDao
) : SavedForLaterRepository {

    override suspend fun insertSavedForLater(savedForLater: SavedForLaterEntity) {
        savedForLaterDao.insertSavedForLater(savedForLater)
    }

    override fun getAllSavedForLater(): Flow<List<SavedForLaterEntity>> {
        return savedForLaterDao.getAllSavedForLater()
    }

    override suspend fun getAllSavedForLaterList(): List<SavedForLaterEntity> {
        return savedForLaterDao.getAllSavedForLaterList()
    }

    override suspend fun getSavedForLaterCount(): Int {
        return savedForLaterDao.getSavedForLaterCount()
    }

    override fun getSavedForLaterCountFlow(): Flow<Int> {
        return savedForLaterDao.getSavedForLaterCountFlow()
    }

    override suspend fun getSavedForLaterById(id: String): SavedForLaterEntity? {
        return savedForLaterDao.getSavedForLaterById(id)
    }

    override suspend fun deleteSavedForLater(id: String) {
        savedForLaterDao.deleteSavedForLater(id)
    }

    override suspend fun deleteAllSavedForLater() {
        savedForLaterDao.deleteAllSavedForLater()
    }
}
