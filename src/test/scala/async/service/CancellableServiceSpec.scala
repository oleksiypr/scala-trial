package async.service

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import java.util.UUID

class CancellableServiceSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  private def setupWorker(jobId: UUID, totalCount: Long): CancellableWorker = {
    val worker = mock[CancellableWorker]
    when(worker.prepare()).thenReturn(IO.pure((jobId, totalCount)))
    when(worker.run(any[UUID])).thenReturn(IO.unit)
    worker
  }

  "CancellableService#start()" should {
    "return JobStarted with a job ID and total count" in {
      val jobId = UUID.fromString("48bf7b76-00aa-4583-b8d6-d63c1830696f")
      val worker = setupWorker(jobId, totalCount = 42L)

      val service = new CancellableService(worker)
      for
        result <- service.start()
        _      <- IO(result.id shouldBe jobId)
        _      <- IO(result.totalCount shouldBe 42L)
      yield
        verify(worker, times(1)).prepare()
        verify(worker, times(1)).run(jobId)
        succeed
    }
  }
}
