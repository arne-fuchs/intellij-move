package org.move.cli.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Disposer
import org.move.cli.Consts
import org.move.cli.defaultMoveSettings
import org.move.cli.runConfigurations.addDefaultBuildRunConfiguration
import org.move.cli.runConfigurations.sui.SuiCliExecutor
import org.move.cli.settings.SuiSettingsPanel
import org.move.ide.newProject.openFile
import org.move.stdext.unwrapOrThrow
import org.sui.openapiext.computeWithCancelableProgress

class MvModuleBuilder : ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> = MvModuleType.INSTANCE

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean = true

    override fun getCustomOptionsStep(
        context: WizardContext,
        parentDisposable: Disposable
    ): ModuleWizardStep {
        return SuiConfigurationWizardStep(context).apply {
            Disposer.register(parentDisposable, this::disposeUIResources)
        }
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
        modifiableRootModel.inheritSdk()

        val suiPath = configurationData?.suiExec?.pathOrNull()
        root.refresh(false, true)
        val isValidSuiCli = ProjectManager.getInstance().defaultMoveSettings?.state?.isValidExec ?: false
        // Just work if user "creates new project" over an existing one.
        if (suiPath != null && isValidSuiCli && root.findChild(Consts.MANIFEST_FILE) == null) {
            val suiCli = SuiCliExecutor(suiPath)
            val project = modifiableRootModel.project
            val packageName = project.name.replace(' ', '_')

            ApplicationManager.getApplication().executeOnPooledThread {
                val manifestFile = project.computeWithCancelableProgress("Generating Sui project...") {
                    suiCli.moveNew(
                        project,
                        modifiableRootModel.module,
                        rootDirectory = root,
                        packageName = packageName
                    )
                        .unwrapOrThrow() // TODO throw? really??
                }
                project.addDefaultBuildRunConfiguration(true)
                project.openFile(manifestFile)
            }
        } else {
            com.intellij.notification.Notifications.Bus.notify(
                Notification(
                    "Move Language",
                    "Create Project Failed",
                    "The sui cli path is invalid",
                    NotificationType.ERROR
                )
            )
        }
    }

    var configurationData: SuiSettingsPanel.PanelData? = null
}
