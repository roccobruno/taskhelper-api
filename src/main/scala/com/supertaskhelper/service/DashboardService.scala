package com.supertaskhelper.service

import com.supertaskhelper.common.enums.TASK_STATUS
import com.supertaskhelper.common.util.PaymentUtil
import com.supertaskhelper.domain._

import scala.collection.JavaConversions

trait DashboardService extends TaskService {

  def loadDashboardData(userId: Option[String]): Dashboard = {



    val aggrResult:Seq[AggrResult] = countTasksWithAggregation(TaskParams(None,
      None,
      userId,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None))

  val tPTasksStats = TPTasksStats(
                                    getResultForStatus(aggrResult,TASK_STATUS.POSTED.toString),
                                    getResultForStatus(aggrResult,TASK_STATUS.ASSIGNED.toString),
                                    getResultForStatus(aggrResult,TASK_STATUS.COMPLETED.toString),
    getResultForStatus(aggrResult,TASK_STATUS.CLOSED.toString),
    getResultForStatus(aggrResult,TASK_STATUS.TOAPPROVEREQUEST.toString),
    getResultForStatus(aggrResult,TASK_STATUS.TOVERIFY.toString))

    val aggrResultForSth:Seq[AggrResult] = countTasksWithAggregation(TaskParams(None,
      None,
      None,
      userId,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None))

    val sTHTasksStats = STHTasksStats(getResultForStatus(aggrResultForSth,TASK_STATUS.POSTED.toString),
      getResultForStatus(aggrResultForSth,TASK_STATUS.ASSIGNED.toString),
      getResultForStatus(aggrResultForSth,TASK_STATUS.COMPLETED.toString),
      getResultForStatus(aggrResultForSth,TASK_STATUS.CLOSED.toString),
      getResultForStatus(aggrResultForSth,TASK_STATUS.TOAPPROVEREQUEST.toString))

    val service = new PaymService
    val obbligations = PaymentUtil.populateFormWithObligationForm(userId.get,
      JavaConversions.seqAsJavaList(findTasks(TASK_STATUS.COMPLETED.toString, userId, None)),
      service)

    val turnover = PaymentUtil.populateModelWithTurnoverForm(userId.get, service)

    var res = Dashboard(tPTasksStats, sTHTasksStats, ObligationToPay(obbligations.getAlreadyPaid, obbligations.getAlreadyApproved,
      obbligations.getToPay, obbligations.getRefunded), Turnover(turnover.getToWithdraw,
      turnover.getComingIncome, turnover.getComingIncomeOnceDone, turnover.getWithdrew))

    logger.info(s"Dashboard:${res}")
    res

  }

  def getResultForStatus(aggrResult: Seq[AggrResult],status:String): Int = {
    val notDefined: AggrResult = AggrResult("NOT_DEFINED",0)
    aggrResult.find(x => (x.status == status)).getOrElse(notDefined).number
  }

  class PaymService extends PaymentService

  def findTasksCount(status: String, userId: Option[String], sthId: Option[String]): Int = {
    var res = findTasks(status, userId, sthId).size
    res
  }

  def buildDiffBids(bids: Seq[Bid]): java.util.List[com.supertaskhelper.common.domain.Bid] = {
    var seq: Seq[com.supertaskhelper.common.domain.Bid] = bids.map(x => buildDiffBid(x)).toSeq
    var list: java.util.List[com.supertaskhelper.common.domain.Bid] = JavaConversions.seqAsJavaList(seq)
    list
  }

  def buildDiffBid(bid: Bid): com.supertaskhelper.common.domain.Bid = {
    var b = new com.supertaskhelper.common.domain.Bid()
    b
  }

  def buildDiffTask(task: Task): com.supertaskhelper.common.domain.Task = {

    var t = new com.supertaskhelper.common.domain.Task()
    t.setBidAcceptedId(task.bidAcceptedId.getOrElse(""))
    t.setBids(buildDiffBids(task.bids.getOrElse(Seq())))

    t
  }

  def findTasks(status: String, userId: Option[String], sthId: Option[String]): Seq[com.supertaskhelper.common.domain.Task] = {
    findTask(TaskParams(None,
      Some(status),
      userId,
      sthId,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None)).map(x => x.get).map(x => buildDiffTask(x))
  }
  def count(status: String, userId: Option[String], sthId: Option[String]): Long = {
    countTasks(TaskParams(None,
      Some(status),
      userId,
      sthId,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None))
  }

}
