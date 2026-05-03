package async.service

import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.effect.testing.scalatest.AsyncIOSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import java.util.UUID
import scala.concurrent.duration.DurationInt

class CancellableServiceSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  private def setupWorker(
      jobId: UUID,
      totalCount: Long,
      runEffect: IO[Unit] = IO.unit
    ): CancellableWorker =
    val worker = mock[CancellableWorker]
    when(worker.prepare()).thenReturn(IO.pure((jobId, totalCount)))
    when(worker.run(any[UUID])).thenReturn(runEffect)
    worker

  "CancellableService#start()" should {
    "return immediately even when run(jobId) is blocked" in {
      val jobId = UUID.fromString("48bf7b76-00aa-4583-b8d6-d63c1830696f")

      for
        runGate <- Deferred[IO, Unit]
        worker   = setupWorker(jobId, totalCount = 42L, runEffect = runGate.get)
        service  = new CancellableService(worker)
        result  <- service.start().timeout(100.millis)
        _       <- IO(result.id shouldBe jobId)
        _       <- IO(result.totalCount shouldBe 42L)
        _       <- IO(verify(worker, times(1)).prepare())
        _       <- IO(verify(worker, timeout(100).times(1)).run(jobId))
      yield
        succeed
    }

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
