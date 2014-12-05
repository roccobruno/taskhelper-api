package com.supertaskhelper.service

import com.supertaskhelper.common.enums.TASK_STATUS
import com.supertaskhelper.common.util.PaymentUtil
import com.supertaskhelper.domain._

import scala.collection.JavaConversions

trait DashboardService extends TaskService {

  def loadDashboardData(userId: Option[String]): Dashboard = {

    val openedTasksTP = findTasksCount(TASK_STATUS.POSTED.toString, userId, None)
    val assignedTasksTP = findTasksCount(TASK_STATUS.ASSIGNED.toString, userId, None)
    val completedTasksTP = findTasksCount(TASK_STATUS.COMPLETED.toString, userId, None)
    val closedTasksTP = findTasksCount(TASK_STATUS.CLOSED.toString, userId, None)
    val requestedTP = findTasksCount(TASK_STATUS.TOAPPROVEREQUEST.toString, userId, None)
    val waitingTP = findTasksCount(TASK_STATUS.TOVERIFY.toString, userId, None)

    val tPTasksStats = TPTasksStats(openedTasksTP, assignedTasksTP, completedTasksTP, closedTasksTP, requestedTP, waitingTP)

    val openedTasksSTH = findTasksCount(TASK_STATUS.POSTED.toString, None, userId)
    val assignedTasksSTH = findTasksCount(TASK_STATUS.ASSIGNED.toString, None, userId)
    val completedTaskSTH = findTasksCount(TASK_STATUS.COMPLETED.toString, None, userId)
    val closedTasksSTH = findTasksCount(TASK_STATUS.CLOSED.toString, None, userId)
    val requestedSTH = findTasksCount(TASK_STATUS.TOAPPROVEREQUEST.toString, None, userId)

    val sTHTasksStats = STHTasksStats(openedTasksSTH, assignedTasksSTH, completedTaskSTH, closedTasksSTH, requestedSTH)

    val service = new PaymService
    val obbligations = PaymentUtil.populateFormWithObligationForm(userId.get,
      JavaConversions.seqAsJavaList(findTasks(TASK_STATUS.COMPLETED.toString, userId, None)),
      service)

    val turnover = PaymentUtil.populateModelWithTurnoverForm(userId.get, service)

var res=    Dashboard(tPTasksStats, sTHTasksStats, ObligationToPay(obbligations.getAlreadyPaid, obbligations.getAlreadyApproved,
      obbligations.getToPay, obbligations.getRefunded), Turnover(turnover.getToWithdraw,
      turnover.getComingIncome, turnover.getComingIncomeOnceDone, turnover.getWithdrew))

    logger.info(s"Dashboard:${res}")
    res

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
}
